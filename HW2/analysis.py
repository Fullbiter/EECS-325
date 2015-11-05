# Kevin Nash (kjn33)
# EECS 325
# Homework 2 Lab

def open_files():
    '''
    Open text files.

    The target files must be named ASCII004 through ASCII904,
    and they must be retrievable from the working directory.
    '''
    files = []        
    for i in range(0, 10):
        files.append(open('ASCII%s04.txt' % i))
    return files

def read_files(files):
    '''
    Read opened text files into strings
    '''
    strings = []
    for f in files:
        strings.append(f.read())
    return strings

def count_resolutions(strings):
    '''
    Compute the average number of DNS resolutions per session
    '''
    resolutions = []
    for s in strings:
        resolutions.append(s.count('CNAME'))
    return resolutions

def count_load_balancers(strings):
    '''
    Estimate whether a load balancer was used to serve the request
    '''
    uses_load_balancer = []
    for s in strings:
        uses_load_balancer.append(s.count('resolver2') > 0)
    return uses_load_balancer

if __name__ == '__main__':
    #  Get useful data from the ten packet capture files
    
    files = open_files()
    strings = read_files(files)

    resolutions = count_resolutions(strings)

    resolution_tot = sum(resolutions)
    resolution_avg = resolution_tot / float(len(strings))

    uses_load_balancer = count_load_balancers(strings)

    print 'There were %s resolutions total' % resolution_tot
    print 'There were %s average resolutions per session' % resolution_avg
    print '%s site(s) used a load balancer' % sum(uses_load_balancer)
