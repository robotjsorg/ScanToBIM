#!/usr/bin/python
import os

def main():
	os.system('javac TCPServer.java')
	os.system('java TCPServer')
	os.system('python reader.py /home/suriya/Scans/')
	os.system('python concat.py')

if __name__ == "__main__":
    main()
