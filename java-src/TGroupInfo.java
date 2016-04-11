/* Anton Dubrau
 * 260171516
 * 24.11.2005
 * 
 * this is an ADT directly associated with Sudoku
 * it stores for a block, row or column
 * how many of its elements could take each of its values,
 * according to the rules that only one each element can only appear once
 * 
 * i.e. for 2x2
 * 1 x 4 x - values 2 and 3 can appear twice each 
 * 
 * I bend the laws of OOP a little bit to make it a little faster -- or something
 * values (the indizes) go from 1 to size
 * internally they are stored 0 to size-1
 * the possibilities go from 0 to size
 * 0 meaning the value is already set in the group
 * size meaning that all elements can take that value
 */
import java.awt.Point;

public class TGroupInfo {
    private int list[];      
    private int size;
	
    public TGroupInfo(int size){
	list = new int[size];
	for (int n = 0; n<size;n++)
	    list[n] = size;    //at the beginning each element has the possiblity to be anything
	this.size = size;
    }
	
    /* decreases the number of possiblities of one value
     * returns the new possibility for value pos */
    public int decrease(int pos){
	pos = pos-1; //correct index
	if (list[pos]<1) return 0;
	list[pos]--;
	return list[pos];
    }
	
	
    /* sets possiblity of a value - i.e. useful for guessing or inserting*/
    public void set(int value, int possibility){
	value = value-1; //correct index;
	list[value] = possibility;
    }
	
    /* returns deepcopy of an object */
    public Object clone(){
	TGroupInfo result = new TGroupInfo(size);
	result.list = (int[])list.clone();
	result.size = size;
	return result;
    }
	
    /* returns whether objects are equivalent */
    public boolean equals(TGroupInfo gi){
	if (this.size != gi.size) return false;
	for (int n=0;n<size;n++){
	    if (list[n]!=gi.list[n]) return false;
	}
	return true;
    }
	
    /* returns lowest possibility (x), and its value (y)*/
    public Point getlowestpossibility(){
	int lowestvalue = size;
	int lowestindex = 0;
	for (int n = 0; n<size; n++){
	    if ((list[n]<lowestvalue)&&(list[n]!=0)){
		lowestvalue = list[n];
		lowestindex = n;
	    }
	}
	return new Point(lowestvalue,lowestindex+1);
    }
	
	
    /* returns the number of possibilites for a certain value */
    public int getpossibility(int value){
	return this.list[value-1]; //adjust index
    }
	
}


