# NHLStatsCSVGenerator

This is a rudementary application that will scan a folder of card images from NHL 21 (PNG, expected to be ```1920x1080``` -- raw from PS4 screenshots), extract statistical data from cards via OCR, and output the results to two CSV files in the same directory.

Example invocation: ```NHLStatsCSVGenerator -d "C:/NHLData/Anaheim Ducks/Anaheim Ducks/" -t "C:/tessdata"```, where:
* ```-d``` specifies the directory to scan for
* ```-t``` specifies the Tesseract training data to use for OCR.  Default training data is sufficient, from [here](https://codeload.github.com/tesseract-ocr/tessdata/zip/master).

Import via IntelliJ and build.  JARs available soon.
