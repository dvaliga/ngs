/*

    ngs-tools  Next generation sequencing (NGS/HTS) command line tools.
    Copyright (c) 2014 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.nmdp.ngs.tools;

import static org.dishevelled.compress.Readers.reader;
import static org.dishevelled.compress.Writers.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;

import org.biojava.bio.program.fastq.Fastq;
import org.biojava.bio.program.fastq.FastqReader;
import org.biojava.bio.program.fastq.SangerFastqReader;
import org.biojava.bio.program.fastq.StreamListener;

import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;

import org.dishevelled.commandline.argument.FileArgument;

/**
 * Convert sequences in FASTQ format to FASTA format.
 */
public final class FastqToFasta implements Runnable {
    private final File fastqFile;
    private final File fastaFile;
    private final FastqReader fastqReader = new SangerFastqReader();
    private static final String USAGE = "java FastqToFasta [args]";


    /**
     * Convert sequences in FASTQ format to FASTA format.
     *
     * @param fastqFile input FASTQ file, if any
     * @param fastaFile output FASTA file, if any
     */
    public FastqToFasta(final File fastqFile, final File fastaFile) {
        this.fastqFile = fastqFile;
        this.fastaFile = fastaFile;
    }


    @Override
    public void run() {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = reader(fastqFile);
            writer = writer(fastaFile);

            final PrintWriter w = writer;
            fastqReader.stream(reader, new StreamListener() {
                    @Override
                    public void fastq(final Fastq fastq)
                    {
                        StringBuilder sb = new StringBuilder(1200);
                        sb.append(">");
                        sb.append(fastq.getDescription());
                        sb.append("\n");
                        sb.append(fastq.getSequence());
                        w.println(sb.toString());
                    }
                });
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        finally {
            try {
                reader.close();
            }
            catch (Exception e) {
                // ignore
            }
            try {
                writer.close();
            }
            catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Main.
     *
     * @param args command line args
     */
    public static void main(final String[] args) {
        Switch help = new Switch("h", "help", "display help message");
        FileArgument fastqFile = new FileArgument("i", "fastq-file", "input FASTQ file, default stdin", false);
        FileArgument fastaFile = new FileArgument("o", "fasta-file", "output FASTA file, default stdout", false);

        ArgumentList arguments = new ArgumentList(help, fastqFile, fastaFile);
        CommandLine commandLine = new CommandLine(args);
        try
        {
            CommandLineParser.parse(commandLine, arguments);
            if (help.wasFound()) {
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(-2);
            }
            new FastqToFasta(fastqFile.getValue(), fastaFile.getValue()).run();
        }
        catch (CommandLineParseException e) {
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
    }
}
