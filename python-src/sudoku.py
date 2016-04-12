import sys
import pulp


def solveSudoku(sudoku):
    N = len(sudoku)
    B = int(round(N**.5)) # number of blocks, size of each block    
    
    # set up lp problem
    problem = pulp.LpProblem('Sudoku', pulp.LpMinimize)
    problem += 0, "objective"
    
    # set up variables
    choice = {}
    for r in range(N):
        for c in range(N):
            for v in range(N):
                choice[r,c,v] = pulp.LpVariable("choice_%d_%d_%d" % (r,c,v), 0, 1, pulp.LpInteger)
    
    # every cell has exactly one value
    for r in range(N):
        for c in range(N):
            problem += (1 == pulp.lpSum(choice[r,c,v] for v in range(N)))

    # each row/colum/block is using every value exactly once
    for c in range(N):
        for v in range(N):
            problem += (1 == pulp.lpSum(choice[r,c,v] for r in range(N)))
    for r in range(N):
        for v in range(N):
            problem += (1 == pulp.lpSum(choice[r,c,v] for c in range(N)))
    for br in range(B):
        for bc in range(B):
            for v in range(N):
                problem += (1 == pulp.lpSum(choice[br*B + r, bc*B + c, v]
                                            for r in range(B)
                                            for c in range(B)))
    
    # initialize choices
    for r in range(N):
        for c in range(N):
            v = sudoku[r][c]
            if v != 0:
                problem += (choice[r,c,v - 1] == 1) # go from 1-counting to 0-counting
    print problem

    # solve the lp
    options = {
        'msg' : True,
        'options' : ['presolve more', 'passpresolve 50',]
    }
    solver = pulp.COIN_CMD(**options)
    if not solver.available():
        solver = pulp.PULP_CBC_CMD(**options)
    if not solver.available():
        raise Exception("no solver available for ILP")
    status = pulp.LpStatus[ problem.solve(solver) ]
    if status != 'Optimal':
        raise Exception("solving lp did not result in 'Optimal', instead got: %s" % status)
    
    # get solved sudoku
    return [
        [ int(round(1 + sum(pulp.value(choice[r,c,v]) * v for v in range(N)))) # back to 1-counting
          for c in range(N) ]
        for r in range(N)
    ]


# yields words from the given string, separated by space (and newline)
def readWords(f):
    for line in f:
        for word in line.split():
            if len(word) > 0:
                yield word


# given a generator of words, returns the next integer or instance of
# 'x' or 'X', for which 0 is returned
def nextIntOrX(words):
    while True:
        w = next(words)
        try:
            return int(w)
        except:
            pass
        if w.lower() == 'x':
            return 0


def readSudoku(f):
    words = readWords(f)
    N = int(next(words))**2
    return [ [ nextIntOrX(words) for col in range(N) ] for row in range(N) ]


def printSudoku(sudoku):
    N = len(sudoku)
    B = int(round(N**.5)) # number of blocks, size of each block
    numDigits = len(str(N))
    
    fmt = "%" + str(numDigits) + "d"
    X = (numDigits - 1) * ' ' + 'x'
    
    def valueString(value):
        return (X if value == 0 else (fmt % value))
    
    lines = "-"*(N*(1 + numDigits) + (B-1)*2 - 1) + "\n"
    print (
        lines.join(
            "".join(
                " | ".join(
                    " ".join(valueString(sudoku[rowBlock*B + row][colBlock*B + col])
                             for col in range(B))
                    for colBlock in range(B))
                + "\n"
                for row in range(B))
            for rowBlock in range(B)))


def main():
    sudoku = readSudoku(sys.stdin)
    solved = solveSudoku(sudoku)
    printSudoku(solved)


if __name__ == "__main__":
    main()
