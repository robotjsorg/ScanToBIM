import glob

read_files = glob.glob("/home/suriya/Scans/*.xyz")

with open("result.xyz", "wb") as outfile:
    for f in read_files:
        with open(f, "rb") as infile:
            outfile.write(infile.read())
