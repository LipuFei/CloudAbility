#
# Copyright (C) 2012  Lipu Fei
#

#
# This program is intended to perform automatic execution of applications on VM
# instances on clouds.
# This program consists of three steps:
#   0. parsing parameters.
#   1. downloading files.
#   2. execution.
#   3. uploading results.
#   4. report statistics.
#

import sys, getopt
import time

# The statistics dictionary
statistics = {}

#
# Use "wget" command to download files.
#
def downloadFiles(url, workPath, fileList):
    startTime = time.time()

    # download files
    print "Start downloading files..."
    for file in fileList:
        # prepare command
        fullUrl = "%s/%s" % (url, file)
        wgetCmd = "wget %s -P %s" % (fullUrl, workPath)

        # execute the command
        result = os.system(wgetCmd)

        # create a zero size file if it cannot be downloaded
        if result != 0:
            fullFilePath = "%s/%s" % (workPath, file)
            touchCmd = "touch %s" % fullFilePath
            os.system(touchCmd)

    endTime = time.time()
    statistics["downloadTime"] = endTime - startTime


#
# Execute the commands.
#
def executeCommands(executionCommands, iteration):
    totalExecTime = []

    print "Start executing commands..."
    for i in range(iteration):
        startTime = time.time()

        for execmd in executionCommands:
            os.system(execmd)

        endTime = time.time()
        totalExecTime.append(endTime - startTime)

    # compute average
    statistics["executionTime"] = totalExecTime.average()


#
# Upload files to the server.
#
def uploadFiles(url, workPath, fileList):
    startTime = time.time()

    # TODO: upload files
    print "Start uploading files..."
    for file in fileList:
        pass

    endTime = time.time()
    statistics["uploadTime"] = endTime - startTime


#
# Prepare and upload statistics.
#
def reportStatisitcs(url, port):
    # construct statistics data
    cmd = "%s:%s;" % ("cmd", "statistics")
    content = "%s=%s;" % ("job", statistics["job"])
    content = "%s=%s;" % ("vm", statistics["vm"])
    content = "%s=%s;" % ("iteration", statistics["iteration"])
    content = "%s=%s;" % ("downloadTime", statistics["downloadTime"])
    content = "%s=%s;" % ("executionTime", statistics["executionTime"])
    content = "%s=%s;" % ("uploadTime", statistics["uploadTime"])
    content = "%s=%s;" % ("totalRunningTime", statistics["totalRunningTime"])

    content = cmd + content

    # connect the server and send the statistics
    print "Start sending statistics..."
    sent = False
    while not sent:
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
            host = "%s:%s" % (url, port)
            sock.connect(host)
            sock.send(content)
            sock.close()
            sent = True
        except:
            print "Error while sending statsitics. Try again..."
            try:
                s.close()
            except:
                pass
    print "Statistics has been sent successfully."

#
# Parse parameters.
#
def parseParameters(argv):
    parameters = {}
    opts, args = getopt.getopt(argv,
        "s:p:u:f:w:e:r:j:v:i:",
        ["server=", "port=", "url=", "files=", "workpath=", "execs=",
         "results=", "job=", "vm=", "iteration="])

    for opt, arg in opts:
        # the address of the server
        if opt in ("-s", "--server"):
            parameters["server"] = arg

        # the port of the server
        elif opt in ("-p", "--port"):
            parameters["port"] = arg

        # the prefix URL for downloading files
        elif opt in ("-u", "--url"):
            parameters["url"] = arg

        # files to download
        elif opt in ("-f", "--files"):
            parameters["files"] = []
            fileList = arg.split(',')
            for file in fileList:
                parameters["files"].append(file.strip())

        # the local path to work in
        elif opt in ("-l", "--workpath"):
            parameters["workpath"] = arg

        # the execution commands
        elif opt in ("-e", "--execs"):
            parameters["execs"] = []
            execList = arg.split(';')
            for execmd in execList:
                parameters["execs"].append(execmd.strip())

        # resulting files to be uploaded to the server
        elif opt in ("-r", "--results"):
            parameters["results"] = []
            fileList = arg.split(',')
            for file in fileList:
                parameters["results"].append(file.strip())

        # name of this job
        elif opt in ("-j", "-job"):
            parameters["job"] = arg

        # the name of this VM instance
        elif opt in ("-v", "--vm"):
            parameters["vm"] = arg

        # number of iterations
        elif opt in ("-i", "--iteration"):
            parameters["iteration"] = arg

        else:
            print "Unknown parameter: \"%s=%s\"." % (opt, arg)

    return parameters

#
# The main function
#
def main(argv):
    startTime = time.time()

    # parse parameters
    parameters = parseParameters(argv)

    # download files
    url = parameters["url"]
    workPath = parameters["workpath"]
    files = parameters["files"]
    downloadFiles(url, workPath, files)

    # execute the commands
    executionCommands = parameters["execs"]
    iteration = parameters["iteration"]
    if iteration == None:
        iteration = 1
    executeCommands(executionCommands, iteration)

    # TODO: upload files
    #results = parameters["results"]
    #uploadFiles(url, workPath, results)

    endTime = time.time()
    statistics["totalRunningTime"] = endTime - startTime

    # prepare and report statistics
    statistics["job"] = parameters["job"]
    statistics["vm"] = parameters["vm"]
    reportStatisitcs(url, port)


if __name__ == "__main__":
    main(sys.argv[1:])
