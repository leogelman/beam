/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* This (groovy-maven-plugin) script finds the supported python interpreter and pip
 * binary in the path. As there is no strict naming convention exists amongst OSes
 * for Python & pip (some call it python2.7, others name it python-2.7),
 * the script tries to execute the candidates and query their version.
 * The first matching interpreter & pip is assigned to "python.interpreter.bin"
 * and "python.pip.bin" (maven) properties respectively.
 */

import org.apache.maven.plugin.MojoFailureException

requiredPythonVersion = /.*[Pp]ython 3\.6.*/

pythonCandidates = ["python3.6", "python-3.6", "python3", "python-3", "python"]
pipCandidates = ["pip3.6", "pip-3.6", "pip3", "pip-3", "pip"]

def String findExecutable(String[] candidates, versionRegex) {
    for (candidate in candidates) {
        try {
            def exec = "${candidate} --version".execute()

            def consoleSB = new StringBuilder()
            exec.waitForProcessOutput(consoleSB, consoleSB)
            consoleStr = consoleSB.toString().replaceAll("\\r|\\n", "")

            if (exec.exitValue() == 0 && consoleStr ==~ versionRegex) {
                return candidate
            }
        } catch (IOException e) {
            continue
        }
    }
    return null
}

def Boolean isWindows() {
    return System.properties['os.name'].toLowerCase(Locale.ROOT).contains('windows');
}

/* On MS Windows applications with dots in the filename can only be executed
 * if the .exe suffix is also included. That is 'pip2.7' will cause an execution error,
 * while 'pip2.7.exe' will succeed (given that pip2.7.exe is an executable in the PATH).
 * The specializeCandidateForOS closure takes care of this conversion.
 */
def specializeCandidateForOS = { it -> isWindows() ? it + '.exe' : it }

pythonBin = findExecutable(pythonCandidates.collect(specializeCandidateForOS) as String[],
                           requiredPythonVersion)
pipBin = findExecutable(pipCandidates.collect(specializeCandidateForOS) as String[],
                        requiredPythonVersion)

if (pythonBin == null) {
   throw new MojoFailureException("Unable to find Python 3.6 in path")
}

if (pipBin == null) {
   throw new MojoFailureException("Unable to find pip for Python 3.6 in path")
}

log.info("Using python interpreter binary '" + pythonBin + "' with pip '" + pipBin + "'")

project.properties.setProperty("python.pip.bin", pipBin)
project.properties.setProperty("python.interpreter.bin", pythonBin)
