Sudoku Learning
===============

This project juxtaposes two Sudoku solvers I wrote:

* One I wrote back in Fall 2005, in my first semester at University. I spent a lot of time on that. It's written in java, and implements a sort of branch and cut algorithm explicitly, using tricks like bit-masks to try to explore the problem space quickly. The code is aware of constraints and keeps them updated when exploring the problem space.

* Another one I wrote early in 2016, in a day or so. This solver models the problem as an integer linear program, and solves it using [PuLP](https://pypi.python.org/pypi/PuLP). This solver is much less clever, but written much more quickly by leveraging the power of an integer linear programming solver.

The point in a way is to showcase the power but maybe also the limits of integer linear programming. The `runall.py` script will run all the supplied puzzles with both solvers, and the result for my machine is in `output.txt`. The ILP solver can solve most problems, and is competitive with the hand-written solver. Only the very large inputs are solved quicker by the hand-written solver, because it can more quickly find solutions for 'easy', large problems.

This is provded for educational purposes. Since it's based on some unlicensed code, and the source for a lot of the puzzles is unknown, I'm also keeping this unlicensed for now.

This may get updated if I get to write a blog post about it.
