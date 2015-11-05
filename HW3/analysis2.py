# Kevin Nash (kjn33)
# EECS 325
# Homework 3 Lab

def open_file():
    '''
    Open text file.

    The target file must be named ASCII804,
    and they must be retrievable from the working directory.
    '''
    textfile = open('ASCII804.txt')
    return textfile

def read_file(files):
    '''
    Read opened text file into strings
    '''
    strings = f.read()
    return strings

def compute_rtt(strings):
    '''
    Compute the average number of DNS resolutions per session
    '''
    rtts = []
    for s in strings:
        rtts.append("""IP""");
    return rtts

if __name__ == '__main__':
    #  Get useful data from the ten packet capture files
    
    textfile = open_file()
    strings = read_file(textfile)

    rtts = compute_rtt(strings)

    for e in rtts
        print str(e)
        