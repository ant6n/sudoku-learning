import os
import glob
import re
import time

TIMEOUT = 1800

# compile
print "compiling java source"
os.system("mkdir java-bin")
os.system("javac -d java-bin java-src/*.java")

print "run all"
for puzzle in glob.glob("puzzles/[89]*.txt"):
    name = re.match("puzzles/(.*)\.txt", puzzle).groups()[0]
    
    # run java
    t = time.time()
    success = 0 == os.system("cat {file} | gtimeout {timeout} java -cp java-bin Sudoku > /dev/null"
                             .format(file=puzzle, timeout=TIMEOUT))
    javaTime = (time.time() - t) if success else float('nan')
    
    # run python
    t = time.time()
    success = 0 == os.system("cat {file} | gtimeout {timeout} python python-src/sudoku.py > /dev/null"
                             .format(file=puzzle, timeout=TIMEOUT))
    pythonTime = (time.time() - t) if success else float('nan')
    
    print "%15s, %10.3f, %10.3f" % (name, javaTime, pythonTime)

