/* Anton Dubrau
 * 260171516
 * 24.11.2005
 *
 * extended from Sudoku.java by Mathieu Blanchette
 * 
 * added private classes 
 * - TCandidates (it doesnt have an extra file because its very intertwined with Sudoku)
 * - TCandidate (thats a tiny support class solely used by TCandidates)
 * - Tgroupcandidate (used inside TCandidate)
 *
 * This algorithm uses backtracking to find the solution.
 * It finds the element with the lowest possibilites 
 * and it finds the value within all groups (block, row, column) so that the least
 * number of elements can take that value.
 * The algorithm decides then which of both ways lead to a decision with fewer possible
 * candidates.
 * 
 * the two other public classes represent these two ideas. the one saves how many
 * elements could take a value in each group.
 * the other saves for each element what values it could take.
 * 
 * its a mess, but it works.
 *
 */
import java.util.*;
import java.io.*;
import java.awt.Point;




class Sudoku {
    /*
     * SIZE is the size parameter of the Sudoku puzzle, and N is the square of
     * the size. For a standard Sudoku puzzle, SIZE is 3 and N is 9.
     */
    int SIZE, N;

    /*
     * The grid contains all the numbers in the Sudoku puzzle. Numbers which
     * have not yet been revealed are stored as 0.
     */
    int Grid[][];

    /*
     * stores possibilities for each element, at the beginning
     * all values are one
     */	
    TBitSet flags; 
	
	
    /* Performance
     * does not get compared using equal() */
    int totalnext=0, totalbranch = 0, valuedecisions=0, elementdecisions=0;
	

    /* stores how many how many elements in each row column and block
     * have the a value as a possibility, for each possible value
     */
    TGroupInfo blocks[], rows[], columns[];
	
    /* sotres where the last inserted or delted element was
     * this doesnt get compared using equal() */
    Point lastinsert=null;
    Point lastgroupinsert=null; 
    /*this class doesnt take care of this membert, TCandidates can play around with it
     * its stored as a point (grouptype,groupindex), where grouptype has the values
     * of the constants defined in TCandidates
     */

    /* stores how many elements are left to set */
    int left;

    /*
     * The solve() method should remove all the unknown characters ('x') in the
     * Grid and replace them with the numbers from 1-9 (for SIZE = 3) that satisfy the Sudoku
     * puzzle.
     * returns how long it took in ms
     */
    public long solve() {
	long time = System.currentTimeMillis();
	next();	
	return System.currentTimeMillis() - time;
    }

    /* finds next element to insert and inserts it - and calls itself
     * recursion stops when all elments are set
     * returns true when solution was found and set
     */
    private boolean next() {
	/* return true if there is no element to set */	
	if (left == 0)return true;
	/* perforamance */
	this.totalnext++;		
	/* create candidatelist */
	TCandidates candidates = new TCandidates(this);
	/* if the elementcout is 1, then value can only be taken by one element in that group
	 * - simply insert 
	 * it is possible that we are pursuing a wrong possibility, so abort if we cant insert*/
	if (candidates.getcount()==1){
	    if (!this.insert(candidates.getcandidatex(0),candidates.getcandidatey(0),candidates.getvalue(0)))
		return false;
	    return next();
	}
	/* here it gets mroe complicated
	 * we have to insert more than one possibilities */
	/* go through all candidates and try inserting them, use a deep copy */
	totalbranch++;
	for (int n=0;n<candidates.getcount();n++){
	    /* try to insert element, and call next if succesful, return true if that was succesful */
            Sudoku copy = (Sudoku)clone();
	    if (this.insert(candidates.getcandidatex(n),candidates.getcandidatey(n),candidates.getvalue(n))){
		if (this.next()) return true;
		/* above line failed get the 'untainted' sudoku back */
		int tn = this.totalnext; int tb = this.totalbranch;
		int e = this.elementdecisions; int v = this.valuedecisions;
		this.assign(copy);
		this.totalnext = tn; this.totalbranch = tb;
		this.elementdecisions = e; this.valuedecisions = v;
	    }
	}
	/* nothing worked, just return false */
	return false;
    }

    /* inserts element and updates group info and flag info
     * returns false if element cant be added */
    private boolean insert(int xpos, int ypos, int value) {
	/* if element is 0 we ignore it, so that insert can be used for filling */
	if (value == 0)
	    return true;
	/* CHECK whether we are allowed to insert
	 * this is a little bit more complicated than expected because the data stores alot of 
	 * information, its easy to corrupt it without even immediately breaking the general rules */
	/* 1st - check whether we try to assign something thats already assigned */
	if (Grid[xpos][ypos]!=0){
	    System.out.print("TRYING TO DOUBLE ASSIGN!\n");
	    return false;
	}
	/* 2nd - check whether the value we want to enter is in that elements possibilites */
	if (!flags.get(xpos,ypos,value)){
	    System.out.print("invalid assign!! Cant assign "+xpos+" "+ypos+" "+value+"\n");
	    print();
	    System.out.print("sudoku "+(isvalid()?"is valid":"is not valid")+"\n");
	    System.out.print("sudoku "+(checkintegrity()?"is integer":"is not integer")+"\n");
	    return false;
	}
	/* 3rd - check whether the element is the only candidate for another value
	 * by checking for all its possibilities whether there is only one possibility
	 * in one of its groups
	 */
	for (int n=1;n<=N;n++){
	    if ((flags.get(xpos,ypos,n))&&(n!=value)){
		if ((rows[ypos].getpossibility(n)==1)||
		    (blocks[xytoblock(xpos,ypos)].getpossibility(n)==1)||
		    (columns[xpos].getpossibility(n)==1)){
		    return false;
		}
	    }
	}
	/* everything ok - the data wont be corrupted (it could still be a false insertion) */
	/* now set the possibilty for value 'value' to zero for each goup */
	blocks[xytoblock(xpos, ypos)].set(value, 0);
	columns[xpos].set(value, 0);
	rows[ypos].set(value, 0);
	/* eleminite all original possibilities of set element */
	for (int n = 1; n <= N; n++) {
	    this.eliminatepossibility(xpos,ypos,n);
	}
	/* eliminate the possibility that value can occur in the groups */
	/* row */
	for (int x = 0; x < N; x++) {
	    this.eliminatepossibility(x,ypos,value);
	}
	/* column */
	for (int y = 0; y < N; y++) {
	    this.eliminatepossibility(xpos,y,value);
	}
	/* block */
	int bx = xpos - xpos % SIZE;
	int by = ypos - ypos % SIZE;
	for (int x = 0; x < SIZE; x++) {
	    for (int y = 0; y < SIZE; y++) {
		this.eliminatepossibility(bx+x,by+y,value);
	    }
	}
	/* insert element set left elements -1, set last set element*/
	Grid[xpos][ypos] = value;
	left--;
        lastinsert = new Point(xpos,ypos);
	return true;
    }

    /* delete method
     * deletes last inserted element one of the elements and updates all data */
    public void delete(int x, int y){
	/* reset how many elements are left */
	left++;
	/* remember deleted */
	int deleted = Grid[x][y];
	/* set flag */
	flags.set(x,y,deleted,true);
	/* set possibilities in groups of element to one for deleted value */
	rows[y].set(deleted,rows[y].getpossibility(deleted)+1);
	columns[x].set(deleted,columns[x].getpossibility(deleted)+1);
	blocks[xytoblock(x,y)].set(deleted,blocks[xytoblock(x,y)].getpossibility(deleted)+1); /**/				    
	/* delete element */
	Grid[x][y] = 0;
	/* go through flags of element and check whether value is unset
	 * in all groups of deleted element, set flag if yes */
	for (int n=1; n<=N; n++){
	    if ((rows[y].getpossibility(n)!=0)
		&&(columns[x].getpossibility(n)!=0)
		&&(blocks[xytoblock(x,y)].getpossibility(n)!=0)
		&&(!flags.get(x,y,n))){
		flags.set(x,y,n,true);
		/* increase possibility in groups */
		rows[y].set(n,rows[y].getpossibility(n)+1);
		columns[x].set(n,columns[x].getpossibility(n)+1);
		blocks[xytoblock(x,y)].set(n,blocks[xytoblock(x,y)].getpossibility(n)+1);
	    }
	}
	/* go through all elements in all of elemetns groups and set the flag of deleted value
	 * to true if they could take the value based on the 2 other goups they are in
	 * if it is not already set (because of earlier command)
	 * and that they dont have a value
	 */
	/* row */
	for (int nx=0;nx<N;nx++){
	    if (x!=nx){
		/* check whether element can take deleted value */
		if ((Grid[nx][y]==0)
		    &&(columns[nx].getpossibility(deleted)!=0)
		    &&(blocks[xytoblock(nx,y)].getpossibility(deleted)!=0)
		    &&(!flags.get(nx,y,deleted))){
		    /* increase possibility, set flag */
		    flags.set(nx,y,deleted,true);
		    rows[y].set(deleted,rows[y].getpossibility(deleted)+1);
		    columns[nx].set(deleted,columns[nx].getpossibility(deleted)+1);
		    blocks[xytoblock(nx,y)].set(deleted,blocks[xytoblock(nx,y)].getpossibility(deleted)+1);
		}
	    }
	}
	/* column */
	for (int ny=0;ny<N;ny++){
	    if (y!=ny){
		/* check whether element can take deleted value */
		if ((Grid[x][ny]==0)
		    &&(rows[ny].getpossibility(deleted)!=0)
		    &&(blocks[xytoblock(x,ny)].getpossibility(deleted)!=0)
		    &&(!flags.get(x,ny,deleted))){
		    /* increase possibility, set flag */
		    flags.set(x,ny,deleted,true);
		    columns[x].set(deleted,columns[x].getpossibility(deleted)+1);
		    rows[ny].set(deleted,rows[ny].getpossibility(deleted)+1);
		    blocks[xytoblock(x,ny)].set(deleted,blocks[xytoblock(x,ny)].getpossibility(deleted)+1);
		}
	    }
	}
	/* block */
	int bx = x - x%SIZE; //offsets
	int by = y - y%SIZE;
	int b = xytoblock(x,y); //block
	for (int nx=0;nx<SIZE;nx++){
	    for (int ny=0;ny<SIZE;ny++){
		if(((bx+nx!=x)&&(by+ny!=y))){ 
		    if ((Grid[bx+nx][by+ny]==0)
			&&(columns[bx+nx].getpossibility(deleted)!=0)
			&&(rows[by+ny].getpossibility(deleted)!=0)
			&&(!flags.get(bx+nx,by+ny,deleted))){
			/* increase possibility, set flag */
			flags.set(bx+nx,by+ny,deleted,true);
			blocks[b].set(deleted,blocks[b].getpossibility(deleted)+1);
			columns[bx+nx].set(deleted,columns[bx+nx].getpossibility(deleted)+1);
			rows[by+ny].set(deleted,rows[by+ny].getpossibility(deleted)+1);
		    }
		}
	    }
	}
	return;
    }
	
	
	
    /* creates a deepcopy of the current sudoku. */
    public Object clone(){
	Sudoku result = new Sudoku(SIZE);
	// clone members and return result
	for (int n=0;n<N;n++)result.Grid[n] = (int[])Grid[n].clone();
	result.flags = (TBitSet)flags.clone();
	//clone groupinfos	    
	for (int n=0;n<N;n++){
	    result.columns[n] = (TGroupInfo)columns[n].clone();
	    result.rows[n] = (TGroupInfo)rows[n].clone();
	    result.blocks[n] = (TGroupInfo)blocks[n].clone();
	}
	result.left = this.left;
	result.lastinsert = (Point)this.lastinsert.clone();
	result.lastgroupinsert = (this.lastgroupinsert==null)?null:(Point)this.lastgroupinsert.clone();
	result.elementdecisions = this.elementdecisions;
	result.totalbranch = this.totalbranch;
	result.totalnext = this.totalnext;
	result.valuedecisions = this.valuedecisions;
	return result;
    }
	
    /* checks whether two sudokus are the same */
    public boolean equals(Sudoku s){
	if ((this.N != s.N)
	    ||(this.SIZE != s.SIZE)
	    ||(this.left != s.left) 
	    ||(!this.flags.equals(s.flags))) return false;
	for (int x=0;x<N;x++){
	    if ((!rows[x].equals(s.rows[x]))
		||(!columns[x].equals(s.columns[x]))
		||(!blocks[x].equals(s.blocks[x]))) return false;
	    for (int y=0;y<N;y++){
		if (Grid[x][y] != s.Grid[x][y]) return false;
	    }
	}
	return true;
    }
	
	
	
    /* the following method takes a sudoku and saves it in the current 
     * this is not a deepcopy!!
     * it merely is equivalent to the statement
     * this = asudoku;
     * but java does not support assigning to self
     */
    public void assign(Sudoku s){
	this.blocks = s.blocks;
	this.columns = s.columns;
	this.flags = s.flags;
	this.Grid = s.Grid;
	this.left = s.left;
	this.N = s.N;
	this.rows = s.rows;
	this.SIZE = s.SIZE;
	this.lastinsert = (Point)s.lastinsert.clone();
	this.lastgroupinsert = (s.lastgroupinsert!=null)?(Point)s.lastgroupinsert.clone():null;
	this.elementdecisions = s.elementdecisions;
	this.totalbranch = s.totalbranch;
	this.totalnext = s.totalnext;
	this.valuedecisions = s.valuedecisions;
    }
	

	
    /* the following method takes one element in the grid and eliminates the 
     * possibility that it can take a certain value
     * if the element did not have that value as a possibility, ignore
     */
    private void eliminatepossibility(int x,int y,int value){
	/* return if value is already no possibility */
	if ((flags.get(x,y,value)==false))return; //||
	// (Grid[x][y] != 0)) return;
	/* set false */
	flags.set(x,y,value,false);
	/* update groupinfo */		
	rows[y].decrease(value);
	columns[x].decrease(value);
	blocks[xytoblock(x,y)].decrease(value);
    }
	
	
    /*
     * the following methods are good to convert from xy coorinates to block
     * convert x,y to block, they are counted from 0
     */
    int xytoblock(int x, int y) {
	return (SIZE * (y / SIZE) + x / SIZE);
    }

    /* convert block to x and y coordinate */
    int blocktox(int block) {
	return (block % SIZE)*SIZE;
    }

    int blocktoy(int block) {
	return (block / SIZE)*SIZE;
    }
	
    /******************************************************************************/
    /* DEBUG FUNCTIONS */
    /* prints the possibilites for a given pixel */
    public void printpossibilites(int x,int y){
	System.out.print("Eelement "+x+","+y+": ");
	for (int n = 1; n<=N;n++){
	    System.out.print((flags.get(x,y,n)?1:0)+", ");
	}
	System.out.print('\n');
    }
	
    /* check whether the sudoku is valid
     * works even when the sudoku has still zeros in it */
    public boolean isvalid(){
	/*columns */
	for (int x=0; x<N; x++)
	    for (int y1=0; y1<N-1; y1++)
		for (int y2=y1+1; y2<N; y2++)
		    if ((Grid[x][y1]==Grid[x][y2])&&(Grid[x][y1]!=0))return false;
       			
       	/* rows */
    	for (int y=0; y<N; y++)
       	    for (int x1=0; x1<N-1; x1++)
       	      	for (int x2=x1+1; x2<N; x2++)
		    if ((Grid[x1][y]==Grid[x2][y])&&(Grid[x1][y]!=0))return false;
       	       	
        /* blocks */
       	for (int block=0;block<SIZE;block++){
	    int bx = blocktox(block);
       	    int by = blocktoy(block);
	    TBitSet values = new TBitSet(1,1); //bitset size 1,1 stores which values have been set
	    for (int x=0; x<SIZE;x++)
		for (int y=0; y<SIZE; y++){
		    if ((Grid[bx+x][by+y]!=0)&&(values.get(0,0,Grid[bx+x][by+y])))return false;
		    values.set(0,0,Grid[bx+x][by+y],true);
		}
       	}
	return true;
    }
	
    /* check integrity of data 
     * check whether flaginfo matches group info
     */
    boolean checkintegrity(){
	/* check whether rows match */
	for (int n=1;n<=N;n++){ //values
	    for (int y=0;y<N;y++){
		int sum=0;
		for (int x=0;x<N;x++){
		    sum+=this.flags.get(x,y,n)?1:0;
		}
		if (sum!=rows[y].getpossibility(n)) return false;
	    }
	}
	/* check columns*/
	for (int n=1;n<=N;n++){ //values
	    for (int x=0;x<N;x++){
		int sum=0;
		for (int y=0;y<N;y++){
		    sum+=this.flags.get(x,y,n)?1:0;
		}
		if (sum!=columns[x].getpossibility(n)) return false;
	    }
	}
	/* check blocks */
	for (int n=1;n<=N;n++){ //values
	    for (int b=0;b<N;b++){
		int sum=0;
		int bx = blocktox(b);
		int by = blocktoy(b);
		for (int x=0;x<SIZE;x++){
		    for (int y=0;y<SIZE;y++){
			sum+=this.flags.get(bx+x,by+y,n)?1:0;}
		}
		if (sum!=blocks[b].getpossibility(n)) return false;
	    }
	}
	/* debug - check whether all set elements have 0 cardinality - 0 possibilities */
	for (int x=0; x<N; x++){
	    for (int y=0; y<N; y++){
		if ((Grid[x][y]!=0)&&(flags.getcardinality(x,y)!=0))
		    System.out.print("match error at "+x+","+y+"\n");
	    }	
	}/* */		
	/* check flags by checking whether each elements' possibility
	 * is possible in each of its groups */
	for (int x=0;x<N;x++){
	    for (int y=0;y<N;y++){
		for (int n=1;n<=N;n++){		
		    if ((flags.get(x,y,n))
			&&(rows[y].getpossibility(n)==0)
			&&(columns[x].getpossibility(n)==0)
			&&(blocks[xytoblock(x,y)].getpossibility(n)==0))
			return false;
		}
	    }
	}
	return true;
    }
	
	

    /** ************************************************************************** */
    /* NOTE: YOU SHOULD NOT HAVE TO MODIFY ANY OF THE FUNCTIONS BELOW THIS LINE. */
    /** ************************************************************************** */
    /*
     * Modifications
     *  - added functionality to read filename from console, usage: 0 <filename>
     *  - constructor
     *  - using insert when reading sudoku
     *  - changed handling of sudoku so that grid is only addressed with x,y
     *  - play around with main function
     */

    /*
     * Default constructor. This will initialize all positions to the default 0
     * value. Use the read() function to load the Sudoku puzzle from a file or
     * the standard input.
     */
    public Sudoku(int size) {
	SIZE = size;
	N = size * size;
	left = N*N;
	flags = new TBitSet(N, N);
	rows = new TGroupInfo[N];
	columns = new TGroupInfo[N];
	blocks = new TGroupInfo[N];

	Grid = new int[N][N];
	for (int i = 0; i < N; i++) {
	    blocks[i] = new TGroupInfo(N);
	    columns[i] = new TGroupInfo(N);
	    rows[i] = new TGroupInfo(N);
	    for (int j = 0; j < N; j++){
		Grid[i][j] = 0;
		/* at the beginning since each element has the possibility to be anything, set flags */
		for (int n=1;n<=N;n++){
		    flags.set(i,j,n,true);
		}
	    }
	}
    }

    /*
     * readInteger is a helper function for the reading of the input file. It
     * reads words until it finds one that represents an integer. For
     * convenience, it will also recognize the string "x" as equivalent to "0".
     */
    static int readInteger(InputStream in) throws Exception {
	int result = 0;
	boolean success = false;

	while (!success) {
	    String word = readWord(in);

	    try {
		result = Integer.parseInt(word);
		success = true;
	    } catch (Exception e) {
		// Convert 'x' words into 0's
		if (word.compareTo("x") == 0) {
		    result = 0;
		    success = true;
		}
		// Ignore all other words that are not integers
	    }
	}

	return result;
    }

    /* readWord is a helper function that reads a word separated by white space. */
    static String readWord(InputStream in) throws Exception {
	StringBuffer result = new StringBuffer();
	int currentChar = in.read();
	String whiteSpace = " \t\n\r";

	// Ignore any leading white space
	while (whiteSpace.indexOf(currentChar) > -1) {
	    currentChar = in.read();
	}

	// Read all characters until you reach white space
	while (whiteSpace.indexOf(currentChar) == -1) {
	    result.append((char) currentChar);
	    currentChar = in.read();
	}
	return result.toString();
    }

    /*
     * This function reads a Sudoku puzzle from the input stream in. The Sudoku
     * grid is filled in one row at at time, from left to right. All non-valid
     * characters are ignored by this function and may be used in the Sudoku
     * file to increase its legibility.
     */
    public void read(InputStream in) throws Exception {
	for (int i = 0; i < N; i++) {
	    for (int j = 0; j < N; j++) {
		this.insert(j, i, readInteger(in));
	    }
	}
    }

    /*
     * Helper function for the printing of Sudoku puzzle. This function will
     * print out text, preceded by enough ' ' characters to make sure that the
     * printint out takes at least width characters.
     */
    void printFixedWidth(String text, int width) {
	for (int i = 0; i < width - text.length(); i++)
	    System.out.print(" ");
	System.out.print(text);
    }

    /*
     * The print() function outputs the Sudoku grid to the standard output,
     * using a bit of extra formatting to make the result clearly readable.
     */
    public void print() {
	// Compute the number of digits necessary to print out each number in
	// the Sudoku puzzle
	int digits = (int) Math.floor(Math.log(N) / Math.log(10)) + 1;

	// Create a dashed line to separate the boxes
	int lineLength = (digits + 1) * N + 2 * SIZE - 3;
	StringBuffer line = new StringBuffer();
	for (int lineInit = 0; lineInit < lineLength; lineInit++)
	    line.append('-');

	// Go through the Grid, printing out its values separated by spaces
	for (int i = 0; i < N; i++) {
	    for (int j = 0; j < N; j++) {
		printFixedWidth(String.valueOf(Grid[j][i]), digits);
		// Print the vertical lines between boxes
		if ((j < N - 1) && ((j + 1) % SIZE == 0))
		    System.out.print(" |");
		System.out.print(" ");
	    }
	    System.out.println();

	    // Print the horizontal line between boxes
	    if ((i < N - 1) && ((i + 1) % SIZE == 0))
		System.out.println(line.toString());
	}
    }

    /*
     * The main function reads in a Sudoku puzzle from the standard input,
     * unless a file name is provided as a run-time argument, in which case the
     * Sudoku puzzle is loaded from that file. It is also possible to load the
     * Sudoku by typing "0 <filename>" in stdin. It then solves the puzzle, and
     * outputs the completed puzzle to the standard output.
     */
    public static void main(String args[]) throws Exception {
	InputStream in;String name = "stdin";
	if (args.length > 0){
	    in = new FileInputStream(args[0]);
	    name = args[0];
	}
	else
	    in = System.in;

	// The first number in all Sudoku files must represent the size of the
	// puzzle. See
	// the example files for the file format.
	int puzzleSize = readInteger(in);
	if (puzzleSize == 0) {
	    name = readWord(in);
	    in = new FileInputStream(name);
	    puzzleSize = readInteger(in);
	}
	if (puzzleSize > 8 || puzzleSize < 1) {
	    System.out
		.println("Error: The Sudoku puzzle size must be between 1 and 8.");
	    System.exit(-1);
	}

	Sudoku s = new Sudoku(puzzleSize);

	// read the rest of the Sudoku puzzle
	s.read(in);
        // Solve the puzzle. We don't currently check to verify that the puzzle
	// can be
	// successfully completed. You may add that check if you want to, but it
	// is not
	// necessary
        int b4 = s.left;
        long t = s.solve();

        /* This is for silent mode
	   if (s.left != 0) System.out.print("Solved with "+s.left+" left\n");
	   else if (!s.isvalid()) System.out.print("Result not valid!\n");
	   else if (!s.checkintegrity()) System.out.print("Result not integer!\n");
	   else System.out.print(name+": "+(t)+" ms - inserted: "+(b4-s.left)+", totalnext: "+s.totalnext+", totalbranch: "+s.totalbranch+"\n");
	   System.out.print("Elementdecisions: "+s.elementdecisions+" Valuedecisions: "+s.valuedecisions+"\n");
	   /**/

	// Print out the (hopefully completed!) puzzle - and some other information
        s.print();
        System.out.print("before, after  "+b4+" "+s.left+"\n");
	System.out.print("sudoku "+(s.isvalid()?"is valid":"is not valid")+"\n");
	System.out.print("sudoku "+(s.checkintegrity()?"is integer":"is not integer")+"\n");
	System.out.print("It took "+t+" Miliseconds!\n");
	System.out.print("totalnext: "+s.totalnext+" totalbranch: "+s.totalbranch+"\n");
	System.out.print("Elementdecisions: "+s.elementdecisions+" Valuedecisions: "+s.valuedecisions+"\n");
	return;
    }
}

		
		

/* this is a class wich holds, after creation, the most possible candidates for entering
 * in the sudoku
 * they are all in the same group (row, column, block)
 * they are all candidates for the same value
 * they are ordered from highest to lowest PROBABILITY
 * if there is only one candidate, than we can be sure the one is part of the solution
 */
class TCandidates{
	
    /* the candidates */
    ArrayList elements; //they are of type TCandidate	
	
    /* this is kind of like an 'enum', there are variables in the code deciding whether a group is
     * s block, row or column - these are their values */
    static final int ROW = 0;
    static final int COLUMN = 1;
    static final int BLOCK = 2;	

    /* Constructor creates the candidates from Sudoku*/ 
    public TCandidates(Sudoku s){
	/* find value with least number of elements that can take it within that group */
	Tgroupcandidate can = findlowestpossibility(s);
	/* if we got a sure candidate, just return that */
	if (can.possibilities == 1){
	    this.makepossibilitylist(s,can.gtype,can.groupindex,can.value);
	    return;
	}
		
	/* find element with least possibilities */
	Point lowc = this.findlowcardcandidate(s);

	/* compare the number of possibilities that we have through groupbased and cardinalitybased
	 * reduction, make list accordingly
	 */
	if (can.possibilities>s.flags.getcardinality(lowc.x,lowc.y)){
	    /* one element taking different values make the candidatelist */
	    if (s.flags.getcardinality(lowc.x,lowc.y) != 1) s.elementdecisions++;//performance test
	    int xpos = lowc.x; int ypos = lowc.y;
	    /* make a list */
	    /* probablity for each value is the least number of possibilities in one of the groups */
	    elements = new ArrayList();
	    for (int n = 1; n<=s.N;n++){
		if (s.flags.get(xpos,ypos,n)){
		    elements.add(new TCandidate(xpos,ypos,n,
						min(s.rows[ypos].getpossibility(n),
						    s.columns[xpos].getpossibility(n),
						    s.blocks[s.xytoblock(xpos,ypos)].getpossibility(n))));
		}
	    }
	    ordercandidates();
	    return;
	} else{
	    /* many elements taking one value make the candidatelist */
	    s.valuedecisions++; //performance test
	    this.makepossibilitylist(s,can.gtype,can.groupindex,can.value);
	    return;
	}
    }//end of constructor
 
	
    /* find in how many same groups two elements are */
    private int samegroups(Sudoku s,int x1, int y1, int x2, int y2){
	int result =0; 
	if (x1==x2) result++;
	if (y1==y2) result++;
	if (s.xytoblock(x1,y1)==s.xytoblock(x2,y2)) result ++;
	return result;
    }	
	
	
    /* orders candidate list according to probablity */
    private void ordercandidates(){
	int n;
	ArrayList ordered = new ArrayList();
	for (int m=0; m<elements.size();m++){
	    TCandidate cur = (TCandidate)elements.get(m);
	    //find position to insert
	    for (n=0;((n<ordered.size())&&(cur.invprobablity<((TCandidate)ordered.get(n)).invprobablity));n++);
	    ordered.add(n,cur);
	}
	elements = ordered;
    }
	
	
    /* find element with lowest cardinality 
     * tries to find one with cloes proximit to last set element*/
    private Point findlowcardcandidate(Sudoku s){
	int lowestc=s.N,xpos=0,ypos=0,ingroups=0;
	/* find element with lowest cadinality */
	for (int x=0;x<s.N;x++){
	    for (int y=0;y<s.N;y++){
		if ((s.flags.getcardinality(x,y)<lowestc)&&(s.flags.getcardinality(x,y)!=0)){
		    lowestc = s.flags.getcardinality(x,y);
		    xpos = x;
		    ypos = y;
		    ingroups = samegroups(s,x,y,s.lastinsert.x,s.lastinsert.y);
		}
		/*if they have the same but the found one is in more groups take that one */
		if ((s.flags.getcardinality(x,y)==lowestc)&&(s.flags.getcardinality(x,y)!=0)){
		    if (samegroups(s,x,y,s.lastinsert.x,s.lastinsert.y)<ingroups){
			lowestc = s.flags.getcardinality(x,y);
			xpos = x;
			ypos = y;
			ingroups = samegroups(s,x,y,s.lastinsert.x,s.lastinsert.y);							
		    }
		}
	    }	
	}	
	return new Point(xpos,ypos);
    }
	
    /* the following function finds the group and value with the lowest possible elements that 
     * can take that value
     */
    private Tgroupcandidate findlowestpossibility(Sudoku s){
	int groupindex=0, lowestposs=s.N, grouptype=ROW, value = 0;
	/* grouptype uses above constants, value refers to the value an element in Sudoku can take */
	/* check row, column, block */
	for (int gtype = ROW;gtype <=BLOCK; gtype++){ /* go through all grouptypes */
	    /* assign current groupinfo list */
	    TGroupInfo[] groups = (gtype==ROW)?s.rows:(gtype==COLUMN?s.columns:s.blocks); 
	    /* loop through groupinfos and set new lowest poss */
	    for (int n=0;n<s.N;n++){
		/* reset lowest possibility if lower found */
		Point cur = groups[n].getlowestpossibility();
		//x lowest possibility, y is which value that refers to
		if (cur.x == 0) System.out.print("POSIBILITY WRONG - 0!!!");
		if (cur.x<lowestposs){
		    lowestposs = cur.x;
		    value      = cur.y;
		    grouptype  = gtype;
		    groupindex = n;
		}
		/* if they are the same but the found one refers to the same value as the last set one
		 * choose that one - this way i hope to find it faster, when we pursue wrong branch */
		if (cur.x==lowestposs){
		    if (((gtype==ROW)&&(s.lastinsert.y == n))
			||((gtype==COLUMN)&&(s.lastinsert.x == n))
			||((gtype==BLOCK)&&(s.xytoblock(s.lastinsert.x,s.lastinsert.y) == n))){
			lowestposs = cur.x;
			value      = cur.y;
			grouptype  = gtype;
			groupindex = n;
		    }
		}
	    }
	} 		
	return new Tgroupcandidate(grouptype,groupindex,value,lowestposs);
    }
	
	
    /* the following function takes a group and a value (which has the lowest possibility in that group)
     * and makes the list of Candidates
     * they are sorted by cardinality
     */
    private void makepossibilitylist(Sudoku s, int grouptype, int groupindex, int value){
	/* find the elements (within that group) that have this value in their possibilities
	 *  and store in list with cardinality (number of possibilities for element)
	 *  as inverse probability*/
	elements = new ArrayList();
	switch (grouptype){
	case ROW:	{for (int x=0;x<s.N;x++){ //the groupindex of row is the y-value
		    if (s.flags.get(x,groupindex,value))
			elements.add(new TCandidate(x,groupindex,value,s.flags.getcardinality(x,groupindex)));
		}break;}
	case COLUMN:{for (int y=0;y<s.N;y++){ //the groupindex of the column is the x-value
		    if (s.flags.get(groupindex,y,value))
			elements.add(new TCandidate(groupindex,y,value,s.flags.getcardinality(groupindex,y)));
		}break;}			 
	case BLOCK:	{for (int x=s.blocktox(groupindex);x<s.blocktox(groupindex)+s.SIZE;x++){
		    for (int y=s.blocktoy(groupindex);y<s.blocktoy(groupindex)+s.SIZE;y++){
			if (s.flags.get(x,y,value))
			    elements.add(new TCandidate(x,y,value,s.flags.getcardinality(x,y)));
		    }
		}break;}			 
	}
	this.ordercandidates();
    }
	
    /* little support function, returns smallest of given arguments */
    private int min(int a,int b,int c){
	int t = (a<b?a:b);
	return (c<t?c:t);
    }
				
    /* Methods for accessing the candidates */
    public int getcount(){
	return elements.size();
    }
    public int getcandidatex(int index){
	return ((TCandidate)elements.get(index)).x;
    }
    public int getcandidatey(int index){
	return ((TCandidate)elements.get(index)).y;
    }
    public int getinvprobability(int index){
	return ((TCandidate)elements.get(index)).invprobablity;
    }
    public int getvalue(int index){
	return ((TCandidate)elements.get(index)).value;
    }
}
		

/* this is a tiny class (structure) defining a candidate. Its only used in class TCandidates
 * the pobablity is stored as inverse of its probablity, "probability" = 1/P(x)*/
class TCandidate{
    int x,y,value,invprobablity;
    TCandidate(int x,int y,int value,int invprobablity){
    	this.x = x; this.y = y; this.value = value; this.invprobablity = invprobablity;
    }
}


/* this tiny class represents the number of possibilities for a value to be taken
 * by an element (how many elements can take a specific value) within one specific group
 */
class Tgroupcandidate{
    int gtype, groupindex, value, possibilities;
    public Tgroupcandidate(int gtype, int groupindex, int value, int possibilities){
	this.gtype = gtype; this.groupindex = groupindex; this.value = value;
	this.possibilities = possibilities;
    }
}









