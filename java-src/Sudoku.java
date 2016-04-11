/* Anton Dubrau
 * 260171516
 * 24.11.2005
 *
 * extended from Sudoku.java by Mathieu Blanchette
 * 
 * added private classes 
 * - TCandidates (it doesnt have an extra file because its very intertwined with Sudoku)
 * - TCandidate (thats a tiny support class solely used by TCandidates)
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

    /* stores how many how many elements in each row column and block
     * have the a value as a possibility, for each possible value
     */
    TGroupInfo blocks[], rows[], columns[];
	
    /* sotres where the last inserted element was */
    int lastx, lasty;

    /* stores how many elements are left to set */
    int left;

    /*
     * The solve() method should remove all the unknown characters ('x') in the
     * Grid and replace them with the numbers from 1-9 (for SIZE = 3) that satisfy the Sudoku
     * puzzle.
     */
    public void solve() {
	next();
    }

    /* finds next element to insert and inserts it - and calls itself
     * recursion stops when all elments are set
     * returns true when solution was found and set
     */
    private boolean next() {
	if (!checkintegrity())
	    System.out.print("INVALID SUDOKU");
	/* return true if there is no element to set */	
	if (left == 0)return true;
	/* debug pring progress */
	if (left == 100){
	    System.out.print("\nleft "+left);
	}
	/* create candidatelist */
	TCandidates candidates = new TCandidates(this);
	/* if the elementcout is 1, then value can only taken by one element in that group
	 * - simply insert 
	 * it is possible that we are pursuing a wrong possibility, so abort if we cant insert*/
	if (candidates.getcount()==1){
	    if (!this.insert(candidates.getcandidatex(0),candidates.getcandidatey(0),candidates.getvalue()))
		return false;
	    candidates = null; //I hope this frees the object
	    return next();
	}
	/* here it gets mroe complicated
	 * we have to insert more than one possibilities */
	/* go through all candidates and try inserting them, use a deep copy */
	//System.out.print("\nbranch at "+left+" with "+candidates.getcount()+" branches: ");
	//for (int n=0;n<candidates.getcount();n++)System.out.print(" "+candidates.getinvprobability(n));
	if (candidates.getinvprobability(0)==1){
	    System.out.print("\nbranch at "+left+" with "+candidates.getcount()+" branches: ");
	    for (int n=0;n<candidates.getcount();n++)System.out.print(" "+candidates.getinvprobability(n));	
	}

	for (int n=0;n<candidates.getcount();n++){
	    Sudoku copy = (Sudoku)this.clone();
	    /* try to insert element, and call next if succesful, return true if that was succesful */
	    if (this.insert(candidates.getcandidatex(n),candidates.getcandidatey(n),candidates.getvalue()))
		if (this.next()) return true;
	    /* above line failed get the 'untainted' sudoku back */
	    this.assign(copy);
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
		    (columns[xpos].getpossibility(n)==1)) 
		    return false;
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
	lastx = xpos;
	lasty = ypos;
	return true;
    }

    /* creates a deepcopy of the current sudoku.
     * this is needed when we try possibilites and want to go back.
     * lets consider a copy functions as opposed to a delete function:
     * for a 6x6 sudoku, we have 1296 elements. even if made
     * a copy for trying each element, we would still get only
     * about 2 million elements, plus extended info, minus
     * the fact that we wont make that many trials 
     * we would probably never use more than 10 MB, which i find still ok
     * so therefore this is much easier and still reasonably fast - everytime
     * there are multiple possibilities (when the logic-part of the algo fails),
     * the sudoku gets saved and solved with each possibility */
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
	result.lastx = this.lastx;
	result.lasty = this.lasty;
	return result;
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
	this.lastx = s.lastx;
	this.lasty = s.lasty;
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
	/* check whether groupinfos match itself */
	/* row */
	for (int gi=0;gi<N;gi++){
	    int l=N,v=0;
	    for (int n=1;n<=N;n++){
		if (rows[gi].getpossibility(n)<l){
		    l = rows[gi].getpossibility(n);
		    v = n;
		}
	    }	
	    if ((v!=rows[gi].getlowestindex())||(v!=rows[gi].getpossibility(l))) return false;
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
     * 
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
		/* since each element has the possibility to be anything, set flags */
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
     * Sudoku puzzle is loaded from that file. It then solves the puzzle, and
     * outputs the completed puzzle to the standard output.
     */
    public static void main(String args[]) throws Exception {
	InputStream in;
	if (args.length > 0)
	    in = new FileInputStream(args[0]);
	else
	    in = System.in;

	// The first number in all Sudoku files must represent the size of the
	// puzzle. See
	// the example files for the file format.
	int puzzleSize = readInteger(in);
	if (puzzleSize == 0) {
	    in = new FileInputStream(readWord(in));
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
	/*s.insert(1,1,1);
	  s.insert(0,0,2);
	  s.insert(0,2,3);
	  s.insert(3,4,2);
	  s.insert(4,4,3);
	*/
        // Solve the puzzle. We don't currently check to verify that the puzzle
	// can be
	// successfully completed. You may add that check if you want to, but it
	// is not
	// necessary.
        int b4 = s.left;
	//s.solve();
	s.print();
	System.out.print("before, after  "+b4+" "+s.left+"\n");
	System.out.print("sudoku "+(s.isvalid()?"is valid":"is not valid")+"\n");
	System.out.print("sudoku "+(s.checkintegrity()?"is integer":"is not integer")+"\n");

	// Print out the (hopefully completed!) puzzle
	// s = (Sudoku)s.clone();
	/* print possibilites for first line
	   for (int n=0; n<s.N; n++)
	   s.printpossibilites(n,0);
	   /* */
	/*check whether blockcoordinates are right
	  for (int x=0; x<s.N; x++){
	  for (int y=0; y<s.N; y++){
			  
				
	  if (s.blocktox(s.xytoblock(x,y))!=x-x%s.SIZE)
	  System.out.print("x error at "+x+","+y+"\n");
	  if (s.blocktoy(s.xytoblock(x,y))!=y-y%s.SIZE)
	  System.out.print("y error at "+x+","+y+"\n");
	  }	
	  System.out.print(s.blocktox(x)+",");
	  System.out.print(s.blocktoy(x)+"\n");
	  System.out.print(s.xytoblock(s.blocktox(x),s.blocktoy(x))+" at "+x+"\n");
			
	  }/* */
	/* debug - check whether all set elements have 0 cardinality - 0 possibilities
	   for (int x=0; x<s.N; x++){
	   for (int y=0; y<s.N; y++){
	   if ((s.Grid[x][y]!=0)&&(s.flags.getcardinality(x,y)!=0))
	   System.out.print("match error at "+x+","+y+"\n");
	   }	
	   }/* */
	/* check validity algorithms
	   System.out.print("sudoku "+(s.isvalid()?"is valid":"is not valid")+"\n");
	   s.insert(3,0,1);
	   System.out.print("sudoku "+(s.isvalid()?"is valid":"is not valid")+"\n");		
	   /* */
	/* find millisecons how long it takes to make a thousand deepcopies
	   long ms = System.currentTimeMillis();
	   s = new Sudoku(8);
	   for (int n=0;n<1000;n++){
	   s = (Sudoku)s.clone();
	   }
	   ms = System.currentTimeMillis() - ms;
	   System.out.print("1000 sudoku copies of 8x8: "+ms+"ms/n");
	   /* */
		
		
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
    /* the value that all candidates are candidates for */
    int value=0;
	
    /* the candidates */
    ArrayList elements; //they are of type Point	
	
    /* this is kind of like an 'enum', there are variables in the code deciding whether a group is
     * s block, row or column - these are their values */
    static final int ROW = 0;
    static final int COLUMN = 1;
    static final int BLOCK = 2;	

    /* Constructor creates the candidates from Sudoku*/
    public TCandidates(Sudoku s){
	/* find group with lowest possibility - highest certainty for certain value - and what value that is*/
	int groupindex=0, lowestposs=s.N, grouptype=ROW;
	/* grouptype uses above constants, value refers to the value an element in Sudoku can take */
	/* check row, column, block */
	for (int gtype = ROW;gtype <=BLOCK; gtype++){ /* go through all grouptypes */
	    /* assign current groupinfo list */
	    TGroupInfo[] groups = (gtype==ROW)?s.rows:(gtype==COLUMN?s.columns:s.blocks); 
	    /* loop through groupinfos and set new lowest poss */
	    for (int n=0;n<s.N;n++){
		/* reset lowest possibility if lower found */
		if (groups[n].getlowestpossibility()<lowestposs){
		    lowestposs = groups[n].getlowestpossibility();
		    value      = groups[n].getlowestindex();
		    grouptype  = gtype;
		    groupindex = n;
		}
		/* if they are the same but the found one refers to the same value as the last one
		 * choose that one - this way i hope to find it faster, when we pursue wrong branch
		 if (groups[n].getlowestpossibility()==lowestposs){
		 if (((gtype==ROW)&&(s.lasty == n))
		 ||((gtype==COLUMN)&&(s.lastx == n))
		 ||((gtype==BLOCK)&&(s.xytoblock(s.lastx,s.lasty) == n))){
		 lowestposs = groups[n].getlowestpossibility();
		 value      = groups[n].getlowestindex();
		 grouptype  = gtype;
		 groupindex = n;
		 }
		 }/**/
		/* if they are the same but found refers to same value choose that one */
		if ((groups[n].getlowestpossibility()==lowestposs)&&
		    (s.Grid[s.lastx][s.lasty]==groups[n].getlowestindex())){
		    lowestposs = groups[n].getlowestpossibility();
		    value      = groups[n].getlowestindex();
		    grouptype  = gtype;
		    groupindex = n;					
		}
	    }
	}
	/* make list */
	elements = makelist(s,groupindex,grouptype,value);
    }//end of constructor

				
    /* the following function takes a group and a value (which has the lowest possibility in that grou)
     * and makes the list of Candidates
     */
    private ArrayList makelist(Sudoku s, int groupindex, int grouptype, int value){
	/* find the elements (within that group) that have this value in their possibilities
	 *  and store temporarily in stack*/
	ArrayList elements = new ArrayList();
	Stack stack = new Stack();
	switch (grouptype){
	case ROW:	{for (int x=0;x<s.N;x++){ //the groupindex of row is the y-value
		    if (s.flags.get(x,groupindex,value))
			stack.add(new Point(x,groupindex));
		}break;}
	case COLUMN:{for (int y=0;y<s.N;y++){ //the groupindex of the column is the x-value
		    if (s.flags.get(groupindex,y,value))
			stack.add(new Point(groupindex,y));
		}break;}			 
	case BLOCK:	{for (int x=s.blocktox(groupindex);x<s.blocktox(groupindex)+s.SIZE;x++){
		    for (int y=s.blocktoy(groupindex);y<s.blocktoy(groupindex)+s.SIZE;y++){
			if (s.flags.get(x,y,value))
			    stack.add(new Point(x,y));
		    }
		}break;}			 
	}
	/* find the inverse probability for each element and order them accordingly */
	Point cur; TCandidate candidate; int n;
	while (!stack.isEmpty()){
	    /* retrieve point */
	    cur = (Point)stack.pop();
	    /* find inverse probablity, 
	     * I define it as the number of possibilities the element has
	     * because the probability that an element should take a certain value is
	     * the inverse of the number of possibilites */
	    candidate = new TCandidate(cur.x,cur.y,value,s.flags.getcardinality(cur.x,cur.y));
	    /* insert with at right position */
	    for (n=0;
		 ((n<elements.size())
		  &&(((TCandidate)elements.get(n)).invprobablity<candidate.invprobablity));n++);
	    elements.add(n,candidate);
	}		
	return elements;
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
    public int getvalue(){
	return value;
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
		
















