// Reached 409/942 place in the Gold League.
// Bot based on MCTS algorithm.
// (Codingame interface requires entire project to be single file)

import java.util.*;

class Pos
{
    int x;
    int y;

    public Pos(int _x, int _y)
    {
        x = _x;
        y = _y;
    }
}

class Board
{
    int player; // 0 or 1
    int[][] gameState; 
    int[][] bigBoard;

    static final int[][][] LINES =
    { // Line positions for winning
        {{0, 0}, {0, 1}, {0, 2}},
        {{1, 0}, {1, 1}, {1, 2}},
        {{2, 0}, {2, 1}, {2, 2}},

        {{0, 0}, {1, 0}, {2, 0}},
        {{0, 1}, {1, 1}, {2, 1}},
        {{0, 2}, {1, 2}, {2, 2}},

        {{0, 0}, {1, 1}, {2, 2}},
        {{0, 2}, {1, 1}, {2, 0}},
    };

    public Board()
    {
        // USAGE: Set player independently
        gameState = new int[9][9];
        bigBoard = new int[3][3];

        for (int i = 0; i < 9; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                this.gameState[i][j] = TicTacToeSimulator.EMPTY;

                if (i < 3 && j < 3)
                    this.bigBoard[i][j] = TicTacToeSimulator.EMPTY;
            }
        }
    }

    public Board copyBoard()
    {
        Board result = new Board();
        result.player = this.player;

        for (int i = 0; i < 9; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                result.gameState[i][j] = this.gameState[i][j];

                if (i < 3 && j < 3)
                    result.bigBoard[i][j] = this.bigBoard[i][j];
            }
        }

        return result;
    }

    public void makeMove(int x, int y)
    {
        player = player ^ 1;

        gameState[x][y] = player;

        int xb = 3 * (x / 3);
        int yb = 3 * (y / 3);
        boolean wonBoard = false;

        for (int[][] pos : LINES)
        {
            int v1 = gameState[xb + pos[0][0]][yb + pos[0][1]];
            int v2 = gameState[xb + pos[1][0]][yb + pos[1][1]];
            int v3 = gameState[xb + pos[2][0]][yb + pos[2][1]];

            if (v1 == player && v1 == v2 && v2 == v3)
            {
                wonBoard = true;
                break;
            }
        }

        if (wonBoard)
        {
            bigBoard[xb / 3][yb / 3] = player;
            for (int i = 0; i < 3; ++i)
            {
                for (int j = 0; j < 3; ++j)
                {
                    gameState[i + xb][j + yb] = player;
                }
            }
        }
    }
}

class Node implements Comparable <Node> { // representing the state of the game

    // int player; // 0 if o's turn has been played, 1 otherwise;
    // player is a part or board;

    Node parent;
    Board gameState; // Representing the board
    Pos move; 
    ArrayList<Node> children;
    double numVisits, UCTValue, victories, draws, losses = 0;
    int winner = TicTacToeSimulator.GAME_CONTINUES; // indicates if node is end game node (game is won, lost or drawn)

    Node (Node p, Board s, Pos m)
    {
        parent = p;
        gameState = s;
        move = m;
        children = new ArrayList<>();
    }

    @Override
    public int compareTo(Node other)
    { // sort nodes in descending order according to their UCT value
        return Double.compare(other.UCTValue, UCTValue);
    }

    void setUCTValue()
    {
        if (numVisits == 0)
            UCTValue = Double.MAX_VALUE; // make sure every child is visited at least once
        else
            UCTValue = ((victories + draws/2) / numVisits) + Math.sqrt(2) * Math.sqrt(Math.log(parent.numVisits) / numVisits);
    }
}

class TicTacToeSimulator {

    static final int EMPTY = -1;
    static final int O = 0;
    static final int X = 1;

    static final int DRAW = 2;
    static final int GAME_CONTINUES = -2;

    Random rand;

    long timeOutDeadLine;

    TicTacToeSimulator()
    {
        rand = new Random();
        rand.setSeed(System.currentTimeMillis());
    }

    int simulateGameFromLeafNode(Node n)
    { // do rollout

        if (n.winner != GAME_CONTINUES) 
            return n.winner; // Check if game is won and node is terminal

        Board currentGameState = n.gameState.copyBoard();

        while (true)
        { // simulate a random game
            ArrayList<Pos> moves = getAllpossibleMoves(currentGameState, n.move);
            int randomMoveIndex = rand.nextInt(moves.size());
            Pos moveToMake = moves.get(randomMoveIndex);
            int player = n.gameState.player; // probably xor;

            currentGameState.makeMove(moveToMake.x, moveToMake.y);
            int won = checkWinOrDraw(currentGameState, player);
            if (won != GAME_CONTINUES)
                return won;
        }
    }

    ArrayList<Pos> getAllpossibleMoves(Board gameState, Pos lastMove)
    {
        ArrayList<Pos> allPossibleMoves = new ArrayList<>();
        int xb = 3 * (lastMove.x % 3);
        int yb = 3 * (lastMove.y % 3);

        if (lastMove.x >= 0)
        {
            for (int i = 0; i < 3; ++i)
            {
                for (int j = 0; j < 3; ++j)
                {
                    if (gameState.gameState[i + xb][j + yb] == EMPTY)
                    {
                        allPossibleMoves.add(new Pos(i + xb, j + yb));
                    }
                }
            }
        }

        if (allPossibleMoves.size() == 0)
        {
            for (int i = 0; i < 9; ++i)
            {
                for (int j = 0; j < 9; ++j)
                {
                    if (gameState.gameState[i][j] == EMPTY)
                    {
                        allPossibleMoves.add(new Pos(i, j));
                    }
                }
            }
        }

        return allPossibleMoves;
    }

    int checkWinOrDraw(Board gameState, int player)
    {
        for (int[][] pos : Board.LINES)
        {
            int v1 = gameState.bigBoard[pos[0][0]][pos[0][1]];
            int v2 = gameState.bigBoard[pos[1][0]][pos[1][1]];
            int v3 = gameState.bigBoard[pos[2][0]][pos[2][1]];

            if (v1 == player && v1 == v2 && v2 == v3)
                return player;
        }

        for (int i = 0; i < 9; ++i)
            for (int j = 0; j < 9; ++j)
                if (gameState.gameState[i][j] == EMPTY)
                    return GAME_CONTINUES;


        int cnt_player = 0;
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 3; ++j)
            {
                if (gameState.bigBoard[i][j] == player)
                    cnt_player++;
                else if (gameState.bigBoard[i][j] == (player^1))
                    cnt_player--;
            }

        return cnt_player > 0 ? player : player^1;
    }
}

class MCTSBestMoveFinder 
{
    TicTacToeSimulator simulator;
    Node rootNode;
    Node bestMove;

    MCTSBestMoveFinder()
    {
        simulator = new TicTacToeSimulator();
    }

    Node selectNodeForRollout()
    { //select

        Node currentNode = rootNode;

        while (true)
        {
            if (currentNode.winner != TicTacToeSimulator.GAME_CONTINUES)
                return currentNode; // if terminal node is selected return it for scoring

            if (currentNode.children.isEmpty())
            {
                generateChildren(currentNode);
                return currentNode.children.get(0);
            }
            else
            {
                for (Node child: currentNode.children)
                {
                    child.setUCTValue();
                }
                Collections.sort(currentNode.children);
                currentNode = currentNode.children.get(0);
                if (currentNode.numVisits == 0)
                {
                    return currentNode;
                }
            }
        }
    }

    void generateChildren(Node n)
    { // expand

        ArrayList<Pos> moves = simulator.getAllpossibleMoves(n.gameState, n.move);
        for (Pos im: moves)
        {
            Board nextGameState = n.gameState.copyBoard();
            int player = nextGameState.player;
            nextGameState.makeMove(im.x, im.y);

            Node child = new Node(n, nextGameState, im); // next player
            child.winner = simulator.checkWinOrDraw(child.gameState, player); // check if this move won
            n.children.add(child);
        }
    }

    void backpropagateRolloutResult(Node n, int won)
    { // backpropagate

        Node current = n;
        while (current != null)
        {
            current.numVisits++;
            if (won == TicTacToeSimulator.DRAW)
            {
                current.draws += 1;
            }
            else if (current.gameState.player == won)
            {
                current.victories+=1;
            }
            else
            {
                current.losses+=1;
            }

            current = current.parent;

        }
    }

    void findBestMove()
    {
        System.gc(); // Launching gargabe collector regularly to make sure it won't start randomly and make programme timeout.

        while (System.currentTimeMillis() <= simulator.timeOutDeadLine)
        {
            Node leafToRollOutFrom = selectNodeForRollout(); // selection / expansion phase
            int won = simulator.simulateGameFromLeafNode(leafToRollOutFrom); // rollout phase
            backpropagateRolloutResult(leafToRollOutFrom, won); // backpropagation phase

        }

        double numVisits = 0; // Iterate over the children of the root node and pick as best move the node which had been visited most often
        for (Node child: rootNode.children)
        {
            if (child.numVisits > numVisits)
            {
                bestMove = child;
                numVisits = child.numVisits;
            }
        }
    }
}

class Player { // Player class

    // Time limit for turn, required by the game (2ms threshold)
    final static long TIME_BUFFER = 98;

    public static void main(String[] args)
    {
        Scanner in = new Scanner(System.in);
        MCTSBestMoveFinder f = new MCTSBestMoveFinder();

        Board initGameState = new Board();
        initGameState.player = TicTacToeSimulator.O;

        int iter = 0;
        int eX = -1, eY = -1;
        Pos startMove = new Pos(-1, -1);

        while (true)
        { // game loop
           
           eX = in.nextInt();
           eY = in.nextInt();
           int validActionCount = in.nextInt();
           for (int i = 0; i < validActionCount; i++)
           {
               in.nextInt(); // x
               in.nextInt(); // y
           }

            f.simulator.timeOutDeadLine = TIME_BUFFER + System.currentTimeMillis();

            if (eX >= 0)
            {
                if (iter == 0)
                {
                    initGameState.makeMove(eX, eY);
                    startMove = new Pos(eX, eY);
                }
                else
                {
                    f.bestMove.gameState.makeMove(eX, eY);
                    f.bestMove.move = new Pos(eX, eY);
                }
            }

            if (iter == 0)
            {
                f.rootNode = new Node(null, initGameState, startMove);
            }
            else
            {
                f.rootNode = new Node(null, f.bestMove.gameState, f.bestMove.move);
            }

            f.findBestMove();

            System.out.println(f.bestMove.move.x + " " + f.bestMove.move.y);

            iter++;
        }
    }
}
