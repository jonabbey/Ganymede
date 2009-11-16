package org.solinger.cracklib;

import java.io.*;

public class Packer {
    
    public static final int MAGIC = 0x70775631;
    public static final int STRINGSIZE = 1024;
    public static final int TRUNCSTRINGSIZE = STRINGSIZE/4;
    public static final int NUMWORDS = 16;
    public static final int MAXWORDLEN = 32;
    public static final int MAXBLOCKLEN = (MAXWORDLEN * NUMWORDS);
    public static final int INTSIZ = 4;

    protected RandomAccessFile dataFile; // data file
    protected RandomAccessFile indexFile; // index file
    protected RandomAccessFile hashFile; // hash file
    protected String mode;

    protected PackerHeader header;

    protected int[] hwms = new int[256];
    protected int count;
    protected String lastWord;
    protected String[] data = new String[NUMWORDS];

    protected int block = -1;
    
    public Packer(String name, String mode) throws IOException {
	this.mode = mode;
	if (!(mode.equals("rw") || mode.equals("r"))) {
	    throw new IllegalArgumentException("Mode must be \"rw\" or \"r\"");
	}

	if (mode.equals("rw")) {
	    // we have to blow it away on write.
	    new File(name+".pwd").delete();
	    new File(name+".pwi").delete();
	    new File(name+".hwm").delete();
	}
	
	dataFile = new RandomAccessFile(name+".pwd",mode); // data file
	indexFile = new RandomAccessFile(name+".pwi",mode); // index file
	try {
	    hashFile = new RandomAccessFile(name+".hwm",mode); // hash file
	} catch (IOException e) {
	    hashFile = null; // hashFile isn't mandatory.
	}
	
	if (mode.equals("rw")) {
	    header = new PackerHeader();
	    header.setMagic(MAGIC);
	    header.setBlockLen((short) NUMWORDS);
	    header.setNumWords(0);
	    
	    // write the header.
	    indexFile.write(header.getBytes());
	} else {
	    
	    header = PackerHeader.parse(indexFile);
	    if (header.getMagic() != MAGIC) {
		throw new IOException("Magic Number mismatch");
	    } else if (header.getBlockLen() != NUMWORDS) {
		throw new IOException("Size mismatch");
	    }
	    
	    // populate the hwms..
	    if (hashFile != null) {
		byte[] b = new byte[4];
		for (int i=0;i<hwms.length;i++) {
		    hashFile.readFully(b);
		    hwms[i] = Util.getIntLE(b);
		}
	    }
	}
    }
    
    public synchronized void close() throws IOException {
	if (mode.equals("rw")) {
	    flush();
	    
	    indexFile.seek(0);
	    indexFile.write(header.getBytes());
	    
	    if (hashFile != null) {
		// Give non-existant letters decent indices.
		for (int i=1; i<=0xff; i++) {
		    if (hwms[i] == 0) {
			hwms[i] = hwms[i-1];
		    }
		}
		for (int i=0;i<hwms.length;i++) {
		    hashFile.write(Util.getBytesLE(hwms[i]));
		}
	    }
	}
	
	indexFile.close();
	dataFile.close();
	if (hashFile != null) {
	    hashFile.close();
	}
    }
    
    public synchronized void put(String s) throws IOException {
	if (!mode.equals("rw")) {
	    throw new IOException("Not opened for write.");
	}
	
	if (s == null) {
	    throw new NullPointerException();
	}

	if (lastWord != null && lastWord.compareTo(s) >= 0) {
	    throw new IllegalArgumentException
		("put's must be in alphabetical order!");
	} else {
	    lastWord = s;
	}

	// truncate if > MAXWORDLEN (including \0)
	data[count] = s.length() > MAXWORDLEN-1 
	    ? s.substring(0,MAXWORDLEN-1) : s;
	    
	hwms[s.charAt(0) & 0xff] = header.getNumWords();
	
	++count;
	header.setNumWords(header.getNumWords()+1);
	
	if (count >= NUMWORDS) {
	  flush();
	}
    }
  
    private synchronized void flush() throws IOException {
	if (!mode.equals("rw")) {
	    throw new IOException("Not opened for write.");
	}

	int index = (int) dataFile.getFilePointer();
	
	// write the pos to the index file.
	indexFile.write(Util.getBytesLE(index));
 
	dataFile.write(data[0].getBytes()); //write null terminated string.
	dataFile.write((byte) 0);
	
	String ostr = data[0];
	
	for (int i = 1; i < NUMWORDS; i++) {
	    String nstr = data[i];
	    
	    if (nstr != null) { //(nstr[0])
		int j = 0;
		for (j = 0; j < ostr.length() && j < nstr.length()
			 && (ostr.charAt(j) == nstr.charAt(j)); j++) {}
		dataFile.write(j & 0xff); // write the index
		dataFile.write(nstr.substring(j).getBytes()); // write the new string from j to end.
	    }
	    dataFile.write((byte) 0); //write a null;
	    
	    ostr = nstr;
	}
	
	data = new String[NUMWORDS];
	count = 0;
    }
    
    public synchronized String get(int num) throws IOException {
	if (!mode.equals("r")) {
	    throw new IOException("Can only get in mode \"r\"");
	}
	
	if (header.getNumWords() <= num) { // too big
	    return null;
	}

	byte[] index = new byte[4];
	byte[] index2 = new byte[4];

	int thisblock = num / NUMWORDS;
	
	if (block == thisblock) {
	    return (data[num % NUMWORDS]);
	}

	//System.out.println("len="+indexFile.length());
	//System.out.println("thisblock="+thisblock);
	
	// get the index of this block.
	indexFile.seek(PackerHeader.sizeOf() + (thisblock * INTSIZ));
	indexFile.readFully(index,0,index.length);

	byte[] buf = null;
	try {
	    // get the index of the next block.
	    indexFile.seek(indexFile.getFilePointer()+INTSIZ);
	    indexFile.readFully(index2,0,index2.length);
	    
	    buf = new byte[Util.getIntLE(index2)-Util.getIntLE(index)];
	} catch (IOException e) { // EOF
	    buf = new byte[MAXBLOCKLEN];
	}
	
	//System.out.println("This index="+Util.getIntLE(index));
	//System.out.println("Next index="+Util.getIntLE(index2));

	// read the data
	dataFile.seek(Util.getIntLE(index));
	Util.readFullish(dataFile,buf,0,buf.length);

	block = thisblock;

	byte[] strbuf = new byte[MAXWORDLEN];
	int a = 0;
	int off = 0;
	
	for (int i=0;i<NUMWORDS;i++) {
	    int b = a;
	    for (;buf[b] != '\0';b++) {}
	    //System.out.println("a="+a+",b="+b+",off="+off);
	    if (b == a) { // not more \0's
		break;
	    }
	    System.arraycopy(buf,a,strbuf,off,(b-a));
	    data[i] = new String(strbuf,0,off+(b-a));
	    //System.out.println(data[i]);
	    a = b+2;
	    off = buf[a-1];
	}
	
	return (data[num % NUMWORDS]);
    }
    
    public int find(String s) throws IOException {
	if (!mode.equals("r")) {
	    throw new IOException("Can only find in mode \"r\"");
	}
	
	int index = (int) s.charAt(0);
	int lwm = index != 0 ? hwms[index - 1] : 0;
	int hwm = hwms[index];

	
	for (;;) {
	    int middle = lwm + ((hwm - lwm + 1) / 2);
	    
	    if (middle == hwm) {
		break;
	    }
	    
	    int cmp = s.compareTo(get(middle));
	    
	    if (cmp < 0) {
		hwm = middle;
	    } else if (cmp > 0) {
		lwm = middle;
	    } else {
		return middle;
	    }
	}
	return -1;
    }

    public int size() {
	return header.getNumWords();
    }

    public static final void usage() {
	System.err.println("Packer -dump <dict> | -make <dict> <wordlist>"
			   + " | -find <dict> <word>");
    }

    public static final void main(String[] args) throws Exception {
	if (args.length == 2 && args[0].equals("-dump")) {
	    Packer p = new Packer(args[1],"r");
	    try {
		for (int i=0;i<p.size();i++) {
		    System.out.println(p.get(i));
		}
	    } finally {
		p.close();
	    }
	} else if (args.length == 3 && args[0].equals("-make")) {
	    Packer p = new Packer(args[1],"rw");
	    try {
		BufferedReader br = new BufferedReader(new InputStreamReader
		    (new FileInputStream(args[2])));
		String s = null;
		while ((s = br.readLine()) != null) {
		    System.out.println("Putting : "+s);
		    p.put(s);
		}
	    } finally {
		p.close();
	    }
	} else if (args.length == 3 && args[0].equals("-find")) {
	    Packer p = new Packer(args[1],"r");
	    try {
		int i = p.find(args[2]);
		if (i != -1) {
		    System.out.println("Found "+p.get(i)+" at "+i);
		} else {
		    System.out.println(args[2]+" not found.");
		}
	    } finally {
		p.close();
	    }
	} else {
	    usage();
	    System.exit(1);
	}
	System.exit(0);
    }
}
