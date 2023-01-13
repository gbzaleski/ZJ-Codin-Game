// Reached 541th place in the Gold League.
// Bot based on heurestic approach, dividing turn in phases and robots in roles.
// Strategy uses a minimal number of recycler and agressive charging against enemy.
// Moves are based on Voronoi/BFS field analysis.
// (Codingame interface requires entire project to be single file)

import java.util.*;
import java.io.*;
import java.math.*;

// Structure for point on the field
class Pos
{
    int x;
    int y;

    public Pos(int _x, int _y)
    {
        x = _x;
        y = _y;
    }

    public String toString()
    {
        return String.format("(%d, %d)", x, y);
    }
}

class Player {

    final static int MAXHEIGHT = 13;
    final static int MAXWIDTH = 25; 
    static int width, height, maxFactories;

    static int[][] scrapAmount = new int[MAXWIDTH][MAXHEIGHT];
    static int[][] owner = new int[MAXWIDTH][MAXHEIGHT]; // 1 = me, 0 = foe, -1 = None
    static int[][] robotsThere = new int[MAXWIDTH][MAXHEIGHT];
    static int[][] recyclerThere = new int[MAXWIDTH][MAXHEIGHT];
    static int[][] willBeSucked = new int[MAXWIDTH][MAXHEIGHT];

    final static int ENEMY = 0;
    final static int ME = 1;
    final static int EMPTY = -1;
    final static int BLOCK = -2;
    final static int TO_PROCESS = -3;

    // Checks if player can move to this position
    private static boolean isFine(int x, int y, int tab[][])
    {
        return 0 <= x && x < width && 0 <= y && y < height && tab[x][y] == EMPTY;
    }
    private static boolean isFine(Pos p, int tab[][])
    {
        return isFine(p.x, p.y, tab);
    }

    private static void calculateEnemyDist(int[][] bfs)
    {
        Queue<Pos> q = new LinkedList<>();
        int dist = 0;
        for (int i = 0; i < width; ++i)
            for (int j = 0; j < height; ++j)
                if (bfs[i][j] == ENEMY)
                    {
                        bfs[i][j] = EMPTY;
                        q.add(new Pos(i, j));
                    }

        while (q.size() > 0)
        {
            int k = q.size();
            while (k-- > 0)
            {
                Pos v = q.remove();
                
                if (isFine(v, bfs))
                {
                    bfs[v.x][v.y] = dist;
                    q.add(new Pos(v.x + 1, v.y));
                    q.add(new Pos(v.x - 1, v.y));
                    q.add(new Pos(v.x, v.y + 1));
                    q.add(new Pos(v.x, v.y - 1));
                }
            }
            dist++;
        }
    }

    private static Pos findNextPos(Pos robot, int[][] bfs)
    {
        int myBfs[][] = new int[width][height];
        Pos res = new Pos(-1, -1);
        int bestDis = (int)1e9;

        for (int y = 0; y < height; y++) 
        {
            for (int x = 0; x < width; x++) 
            {
                myBfs[x][y] = bfs[x][y] == BLOCK ? BLOCK : EMPTY;
            }
        }

        Queue<Pos> q = new LinkedList<>();
        q.add(robot);

        boolean finish = false;
        while (q.size() > 0 && !finish)
        {
            int k = q.size();
            while (k-- > 0)
            {
                Pos v = q.remove();
                
                if (isFine(v, myBfs))
                {
                    myBfs[v.x][v.y] = BLOCK;

                    if (owner[v.x][v.y] == ME)
                    {
                        q.add(new Pos(v.x + 1, v.y));
                        q.add(new Pos(v.x - 1, v.y));
                        q.add(new Pos(v.x, v.y + 1));
                        q.add(new Pos(v.x, v.y - 1));
                    }
                    else
                    {
                        finish = true;

                        if (bfs[v.x][v.y] < bestDis)
                        {
                            bestDis = bfs[v.x][v.y];
                            res = v;
                        }
                    }
                }
            }

        }

        if (res.x != -1)
            owner[res.x][res.y] = ME;

        return res;
    }

    private static Pos findNearestEnemy(Pos robot, int[][] bfs)
    {
        int myBfs[][] = new int[width][height];
        Pos res = new Pos(-1, -1);
        int bestDis = (int)1e9;

        for (int y = 0; y < height; y++) 
        {
            for (int x = 0; x < width; x++) 
            {
                myBfs[x][y] = bfs[x][y] == BLOCK ? BLOCK : EMPTY;
            }
        }

        Queue<Pos> q = new LinkedList<>();
        q.add(robot);
        int curDis = 0;

        while (q.size() > 0)
        {
            int k = q.size();
            while (k-- > 0)
            {
                Pos v = q.remove();
                
                if (isFine(v, myBfs))
                {
                    myBfs[v.x][v.y] = BLOCK;

                    q.add(new Pos(v.x + 1, v.y));
                    q.add(new Pos(v.x - 1, v.y));
                    q.add(new Pos(v.x, v.y + 1));
                    q.add(new Pos(v.x, v.y - 1));
                    

                    if (bfs[v.x][v.y] + curDis < bestDis)
                    {
                        res = v;
                        bestDis = bfs[v.x][v.y] + curDis;
                    }
                    
                }
            }

            curDis++;
        }

        return  res;
    }

    private static int subValue(int x, int y)
    {
        if (0 <= x && x < width && 0 <= y && y < height && willBeSucked[x][y] == 0)
        {
            return scrapAmount[x][y];
        }

        return 0;
    }

    private static int fieldValue(int x, int y)
    {
        int value = scrapAmount[x][y];
        int upperBoundValue = scrapAmount[x][y] * 5;

        value += subValue(x + 1, y);
        value += subValue(x - 1, y);
        value += subValue(x, y + 1);
        value += subValue(x, y - 1);

        return Math.min(value, upperBoundValue);
    }

    public static void main(String args[]) 
    {
        Scanner in = new Scanner(System.in);
        width = in.nextInt();
        height = in.nextInt();

        maxFactories = 3 + (width * height > 200 ? 1 : 0);

        ArrayList<Pos> robots = new ArrayList<>();
        ArrayList<Pos> myFields = new ArrayList<>();

        // Game loop
        while (true) 
        {
            int myMatter = in.nextInt();
            int oppMatter = in.nextInt();
            robots.clear();
            myFields.clear();

            int myBots = 0;
            int enemyBots = 0;
            int myFactories = 0;

            for (int y = 0; y < height; y++) 
            {
                for (int x = 0; x < width; x++) 
                {
                    int _scrapAmount = in.nextInt();
                    scrapAmount[x][y] = _scrapAmount;

                    int _owner = in.nextInt(); // 1 = me, 0 = foe, -1 = neutral
                    owner[x][y] = _owner;
                    if (_owner == ME)
                        myFields.add(new Pos(x, y));

                    int units = in.nextInt();
                    
                    robotsThere[x][y] = units;
                    if (_owner != ME)
                    {
                        enemyBots += units;
                    }

                    while (_owner == ME && units-- > 0)
                    {
                        robots.add(new Pos(x, y));
                        myBots++;
                    }

                    int recycler = in.nextInt();
                    recyclerThere[x][y] = recycler;
                    if (recycler == 1 && _owner == ME)
                        myFactories++;


                    int canBuild = in.nextInt();
                    int canSpawn = in.nextInt();

                    int inRangeOfRecycler = in.nextInt();
                    willBeSucked[x][y] = (inRangeOfRecycler == 1 && _owner == ME ? 1 : 0);
                }
            }

            // Moving 
            int bfsDist[][] = new int[width][height];
            for (int y = 0; y < height; y++) 
            {
                for (int x = 0; x < width; x++) 
                {
                    bfsDist[x][y] = EMPTY;

                    if (owner[x][y] == ENEMY)
                        bfsDist[x][y] = ENEMY;

                    if (scrapAmount[x][y] == 0 || recyclerThere[x][y] == 1)
                        bfsDist[x][y] = BLOCK;
                }
            }
            calculateEnemyDist(bfsDist);

            int bfsUnitEnemy[][] = new int[width][height];
            for (int y = 0; y < height; y++) 
            {
                for (int x = 0; x < width; x++) 
                {
                    bfsUnitEnemy[x][y] = EMPTY;

                    if (owner[x][y] == ENEMY && robotsThere[x][y] > 0)
                        bfsUnitEnemy[x][y] = ENEMY;

                    if (scrapAmount[x][y] == 0 || recyclerThere[x][y] == 1)
                        bfsUnitEnemy[x][y] = BLOCK;
                }
            }
            calculateEnemyDist(bfsUnitEnemy);

            for (Pos robot_i : robots)
            {
                Pos newPos = findNextPos(robot_i, bfsDist);

                int qToMove = robotsThere[robot_i.x][robot_i.y];
                if (qToMove == 0)
                    continue;

                if (newPos.x != -1)
                {
                    int toMove = 1;
                    toMove = Math.max(1, qToMove - 2);

                    System.out.print(String.format("MOVE %d %d %d %d %d;", toMove, robot_i.x, robot_i.y, newPos.x, newPos.y));
                    robotsThere[robot_i.x][robot_i.y]--;
                }
                else 
                {
                    newPos = findNearestEnemy(robot_i, bfsUnitEnemy);
                    if (newPos.x != -1)
                    {
                        System.out.print(String.format("MOVE %d %d %d %d %d;", qToMove, robot_i.x, robot_i.y, newPos.x, newPos.y));
                        robotsThere[robot_i.x][robot_i.y] = 0;
                    }
                }
      
            }

            // Getting recyclers (cost 10)
            while (true) //(myMatter / 10 + myBots - 1 > enemyBots || myFactories == 0) // more conditions to cosnider
            {
                Pos nextFactory = new Pos(-1, -1);
                int bestValue = 15;

                for (Pos owned : myFields)
                {
                    int x = owned.x;
                    int y = owned.y;
                    if (owner[x][y] == ME && willBeSucked[x][y] == 0 && recyclerThere[x][y] == 0)
                    {
                        int thisValue = fieldValue(x, y);

                        if (thisValue > bestValue)
                        {
                            bestValue = thisValue;
                            nextFactory = new Pos(x, y);
                        }
                    }
                }

                if (myMatter > 79 || myMatter < 10 || nextFactory.x == -1 || myFactories >= maxFactories)
                    break;

                System.out.print(String.format("BUILD %d %d;", nextFactory.x , nextFactory.y));
                myFactories++;
                myMatter -= 10;
                recyclerThere[nextFactory.x][nextFactory.y] = 1;

                willBeSucked[nextFactory.x][nextFactory.y] = 1;
                if (nextFactory.x > 0)
                    willBeSucked[nextFactory.x - 1][nextFactory.y] = 1;

                if (nextFactory.x + 1 < width)
                    willBeSucked[nextFactory.x + 1][nextFactory.y] = 1;

                if (nextFactory.y > 0)
                    willBeSucked[nextFactory.x][nextFactory.y - 1] = 1;

                if (nextFactory.y + 1 < height)
                    willBeSucked[nextFactory.x][nextFactory.y + 1] = 1;
            }

            // Spawning (cost 10)
            if (myMatter > 9)
            {
                Pos nextUnits = new Pos(-1, -1);
                int disFromEnemy = (int)1e9;

                for (Pos owned : myFields)
                {
                    int x = owned.x;
                    int y = owned.y;

                    if (owner[x][y] == ME && recyclerThere[x][y] == 0)
                    {
                        if (bfsUnitEnemy[x][y] < disFromEnemy && bfsUnitEnemy[x][y] >= 0)
                        {
                            disFromEnemy = bfsUnitEnemy[x][y];
                            nextUnits = new Pos(x, y);
                        }
                    }
                }

                int lim = (int)1e9;
                if (nextUnits.x == -1)
                {
                    lim = 1;
                    for (int y = 0; y < height; y++) 
                    {
                        for (int x = 0; x < width; x++) 
                        {
                            if (owner[x][y] == ME && robotsThere[x][y] == 0)
                            {
                                if (x - 1 >= 0 && owner[x - 1][y] != ME && scrapAmount[x - 1][y] > 0)
                                {
                                    nextUnits = new Pos(x, y);
                                    break;
                                }

                                if (x + 1 < width && owner[x + 1][y] != ME && scrapAmount[x + 1][y] > 0)
                                {
                                    nextUnits = new Pos(x, y);
                                    break;
                                }

                                if (y - 1 >= 0 && owner[x][y - 1] != ME && scrapAmount[x][y - 1] > 0)
                                {
                                    nextUnits = new Pos(x, y);
                                    break;
                                }
                                
                                if (y + 1 < height && owner[x][y + 1] != ME && scrapAmount[x][y + 1] > 0)
                                {
                                    nextUnits = new Pos(x, y);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (nextUnits.x != -1)
                    System.out.print(String.format("SPAWN %d %d %d;", Math.min(myMatter / 10, lim),  nextUnits.x , nextUnits.y));
            }

            System.out.println("WAIT");
        }
    }
}