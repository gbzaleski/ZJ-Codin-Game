
// Reached 170/635 place in the Legendary League.
// Bot based on minimax algorithm with value function based on Voronoi diagrams and BTS reachability.
// (Codingame interface requires entire project to be single file)

import java.util.*;
import java.io.*;
import java.math.*;

class Pos
{
    int x;
    int y;
    int owner;
    public Pos(int _x, int _y)
    {
        x = _x;
        y = _y;
        owner = -1;
    }

    public Pos(int _x, int _y, int _o)
    {
        x = _x;
        y = _y;
        owner = _o;
    }

    public String toString()
    {
        if (owner != -1)
            return String.format("(%d, %d) of %d", x, y, owner);
        return String.format("(%d, %d)", x, y);
    }
}

class PosDir extends Pos
{
    int dir;

    public PosDir(int _x, int _y, int _d)
    {
        super(_x, _y);
        dir = _d;
    }

    public String toString()
    {
        return String.format("(%d, %d) %s", x, y, Player.dirNames[dir]);
    }
}

class Player {

    final public static String[] dirNames = {"0", "1", "LEFT", "RIGHT", "UP", "DOWN"};
    final public static int LEFT = 2;
    final public static int RIGHT = 3;
    final public static int UP = 4;
    final public static int DOWN = 5;

    final public static int WIDTH = 30;
    final public static int HEIGHT = 20;
    final public static int EMPTY = -1;
    final public static int BLOCK = -2;
    final public static int CONFUSED = -4;
    final public static int DEF_DEPTH = 5;
    final public static int INF = (int)1e9;

    static int[] pX = new int[4];
    static int[] pY = new int[4];
    static boolean[] isDead = new boolean[4];
    static int myInd;
    static int numPlayers;

    // Checks if player can move to this position
    private static boolean isFine(int x, int y, int tab[][])
    {
        return 0 <= x && x < WIDTH && 0 <= y && y < HEIGHT && tab[x][y] == EMPTY;
    }
    private static boolean isFine(Pos p, int tab[][])
    {
        return isFine(p.x, p.y, tab);
    }

    // Generates all avaiable moves
    private static ArrayList<PosDir> availableMove(int mX, int mY, int tab[][])
    {
        ArrayList<PosDir> moves = new ArrayList<>();

        if (isFine(mX, mY - 1, tab))
            moves.add(new PosDir(mX, mY - 1, UP));
        if (isFine(mX - 1, mY, tab))
            moves.add(new PosDir(mX - 1, mY, LEFT));
        if (isFine(mX, mY + 1, tab))
            moves.add(new PosDir(mX, mY + 1, DOWN));
        if (isFine(mX + 1, mY, tab))
            moves.add(new PosDir(mX + 1, mY, RIGHT));

        return moves;
    }

    // Calculate value of current position (minimax)
    public static int calcTable(int tab[][], int nowPlayer)
    {
        int bfs[][] = new int[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; ++x)
            for (int j = 0; j < HEIGHT; ++j)
            {
                bfs[x][j] = tab[x][j] == EMPTY ? EMPTY : BLOCK;
            }

        Queue<Pos> q = new LinkedList<>();

        for (int i = nowPlayer; i < 4; ++i)
        {
            if (isDead[i] == false)
            {
                bfs[pX[i]][pY[i]] = EMPTY;
                q.add(new Pos(pX[i], pY[i], i));
            }
        }
        for (int i = 0; i < nowPlayer; ++i)
        {
            if (isDead[i] == false)
            {
                bfs[pX[i]][pY[i]] = EMPTY;
                q.add(new Pos(pX[i], pY[i], i));
            }
        }

        while (q.size() > 0)
        {
            Pos v = q.remove();
            
            if (isFine(v, bfs))
            {
                bfs[v.x][v.y] = v.owner;
                q.add(new Pos(v.x + 1, v.y, v.owner));
                q.add(new Pos(v.x - 1, v.y, v.owner));
                q.add(new Pos(v.x, v.y + 1, v.owner));
                q.add(new Pos(v.x, v.y - 1, v.owner));
            }
        }

        int myCells = 0;
        int enemyCells = 0;

        for (int x = 0; x < WIDTH; ++x)
        {
            for (int j = 0; j < HEIGHT; ++j)
            {
                int cell = bfs[x][j];
                
                if (0 <= cell && cell < 4)
                {
                    if (cell == myInd)
                        myCells++;
                    else 
                        enemyCells++;
                }
            }
        }
        
        // Weights for parameters can be adjusted/modified.
        return 10000 * myCells - 10 * enemyCells - 0 * availableMove(pX[myInd], pY[myInd], tab).size();
    }

    private static PosDir passDir;

    private static int minimax(int depth, int pId, int tab[][], PosDir bestMove, int alpha, int beta)
    {
        while (isDead[pId])
            pId = (pId + 1) % numPlayers;

        boolean maximisingPlayer = (pId == myInd);
        ArrayList<PosDir> moves = availableMove(pX[pId], pY[pId], tab);

        if (depth == 0)
            return calcTable(tab, pId);

        if (maximisingPlayer)
        {
            int eval = -INF;
            for (PosDir m : moves)
            {
                tab[m.x][m.y] = pId;
                int tmpX = pX[pId];
                int tmpY = pY[pId];
                pX[pId] = m.x;
                pY[pId] = m.y;

                int childEvaled = minimax(depth - 1, (pId + 1) % numPlayers, tab, bestMove, alpha, beta);

                if (childEvaled > eval)
                {
                    eval = childEvaled;
                    if (depth == DEF_DEPTH)
                    {
                       bestMove = new PosDir(m.x, m.y, m.dir);
                       passDir = m;
                    }
                }

                tab[m.x][m.y] = EMPTY;
                pX[pId] = tmpX;
                pY[pId] = tmpY;

                // Alpha Beta Pruning
                alpha = Math.max(alpha, eval);
                if (beta <= alpha)
                    break;
            }

            return eval;
        }
        else // other players
        {
            int eval = INF;

            for (PosDir m : moves)
            {
                int tmpX = pX[pId];
                int tmpY = pY[pId];
                pX[pId] = m.x;
                pY[pId] = m.y;
                tab[m.x][m.y] = pId;

                int childEvaled = minimax(depth - 1, (pId + 1) % numPlayers, tab, bestMove, alpha, beta);

                tab[m.x][m.y] = EMPTY;
                pX[pId] = tmpX;
                pY[pId] = tmpY;

                if (childEvaled < eval)
                {
                    eval = childEvaled;
                }

                beta = Math.min(beta, eval);

                // Alpha Beta Pruning
                if (beta <= alpha)
                    break;
            }

            return eval;
        }
    }

    public static void main(String args[])
    {
        Scanner in = new Scanner(System.in);
        int tab[][] = new int[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; ++x)
            for (int j = 0; j < HEIGHT; ++j)
                tab[x][j] = EMPTY;

        boolean firstRun = true;

        // Game loop
        while (true) 
        {
            numPlayers = in.nextInt(); // total number of players (2 to 4).
            myInd = in.nextInt(); // your player number (0 to 3).

            if (firstRun)
            {
                for (int i = 0; i < 4; ++i)
                    isDead[i] = true;

                firstRun = false;
                for (int i = 0; i < numPlayers; ++i)
                    isDead[i] = false;
            }

            for (int i = 0; i < numPlayers; i++) 
            {
                int X0 = in.nextInt(); // starting X coordinate of lightcycle (or -1)
                int Y0 = in.nextInt(); // starting Y coordinate of lightcycle (or -1)
                int X1 = in.nextInt(); // starting X coordinate of lightcycle (can be the same as X0 if you play before this player)
                int Y1 = in.nextInt(); // starting Y coordinate of lightcycle (can be the same as Y0 if you play before this player)
            
                if (X0 == -1 && isDead[i] == false)
                {
                    System.err.println(i + " died!!");
                    isDead[i] = true;

                    for (int x = 0; x < HEIGHT; ++x)
                    {
                        for (int j = 0; j < WIDTH; ++j)
                        {
                            if (tab[j][x] == i)
                            {
                                tab[j][x] = EMPTY;
                            }
                        }
                    }
                }

                if (X0 >= 0 && isDead[i] == false)
                {
                    tab[X0][Y0] = i;
                    tab[X1][Y1] = i;
                }

                pX[i] = X1;
                pY[i] = Y1;
            }

            PosDir bestMove = new PosDir(-1, -1, -1);
            minimax(DEF_DEPTH, myInd, tab, null, -INF, INF);
            bestMove = passDir;

            System.out.println(dirNames[bestMove.dir]);
            tab[bestMove.x][bestMove.y] = myInd;
        }
    }
}