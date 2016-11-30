import glob

read_files = glob.glob("C:\Users\Joseph\Documents\GitHub\\vulcan-docs\Scans\*.xyz")

with open("result.xyz", "wb") as outfile:
    for f in read_files:
        with open(f, "rb") as infile:
            outfile.write(infile.read())
