import glob

read_files = glob.glob("~/Documents/vulcan-docs/Scans/*.xyz")

with open("~/Documents/vulcan-docs/Scans/result.xyz", "wb") as outfile:
    for f in read_files:
        with open(f, "rb") as infile:
            outfile.write(infile.read())
