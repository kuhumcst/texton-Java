DK-ClarinTools
==============

Web application that computes candidate tool workflows given input file(s) and the user's requirements regarding the output. Afterwards, the application runs the workflow that the user has selected from the list of candidates.

This web application composes workflows from building blocks. Each building block encapsulates a Natural Language Processing tool.

The application may compose many workflows that all lead to the stated goal. In general, the more detailed the goal, the fewer solutions the application will find, even zero.

The original version of the application was made during the [DK-Clarin project](https://dkclarin.ku.dk/). The application is written in the Bracmat programming language, except for the communication with the user's browser and with the tools (web services in their own right), which is implemented in Java.
