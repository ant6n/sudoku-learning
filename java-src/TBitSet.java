/* Anton Dubrau
 * 260171516
 * 24.11.2005
 * 
 * a two dimensional array of flaglists.
 * Each flaglist has 64 entries, its indizes go from 1 to 64.
 * each flag can be set, returned. also, it can be asked how many
 * bits are set true in each flaglist
 * 
 * a flaglist is a 64 bit integer.
 * this class does no internal error checking.
 * its supposed to be pretty fast.
 */


public class TBitSet {
    long flags[][]; 			//the actual data
    short cardinalities[][];  //stores how many bits are set in each flaglist
    long bits[];               //internal bits-list, i.e. 00000001, 00000010, 00000100 ... used for bitoperations
    int xsize, ysize;
  
    /* constructor */
    TBitSet(int xsize, int ysize){
	if ((xsize<0)||(ysize<0)) return;
	flags = new long[xsize][ysize];
	cardinalities = new short[xsize][ysize];
	this.xsize = xsize; this.ysize = ysize;
	for (int x=0;x<xsize;x++){
	    for (int y=0;y<ysize;y++){
		flags[x][y] = 0;
		cardinalities[x][y] = 0;
	    }
	}
	/* make bits array */
	long onebit = 1;
	bits = new long[65];
	bits[0] = 0;
	for (int n=1; n<65; n++){
	    bits[n] = onebit;
	    onebit = onebit << 1; /* shift bit one to the left */
	}
     
    }
  
  
    /* sets bit */
    public void set(int x, int y, int bit, boolean value){
	if (value == true){
	    /* check whether this bit has not been set already, increase cardinality if not*/
	    if ((bits[bit] & flags[x][y])==0){
		cardinalities[x][y]++;
	    }
	    /* set bit */
	    this.flags[x][y] = flags[x][y] | bits[bit];
	} else {
	    /* check whether this bit is already false, decrease cardinality if not */
	    if ((bits[bit] & flags[x][y])!=0){
		cardinalities[x][y]--;
	    }
	    /* set bit */
	    this.flags[x][y] = flags[x][y] & (~bits[bit]);
	}	  
    }
  
    /* get bit*/
    public boolean get(int x, int y, int bit){
	return ((flags[x][y] & bits[bit])!=0);
    }
  
    /* returns how many bits the flat at x,y has set */
    public short getcardinality(int x, int y){
	return cardinalities[x][y];
    }
  
    /* clones bitset */
    public Object clone(){
	TBitSet result = new TBitSet(xsize,ysize);
	for (int x=0;x<xsize;x++){
	    result.cardinalities[x] = (short[])this.cardinalities[x].clone();
	    result.flags[x] = (long[])this.flags[x].clone();
	}
	return result;
    }
  
    /* returns whether data of given object is equivalent */
    public boolean equals(TBitSet bs){
	if ((this.xsize != bs.xsize)
	    ||(this.ysize != bs.ysize)) return false;
	for (int x=0;x<xsize ;x++){
	    for (int y=0;y<ysize;y++){
		if ((this.cardinalities[x][y] != bs.cardinalities[x][y])
		    ||(this.flags[x][y] != bs.flags[x][y])) return false;
	    }
	}
	return true;
    }
  
}







