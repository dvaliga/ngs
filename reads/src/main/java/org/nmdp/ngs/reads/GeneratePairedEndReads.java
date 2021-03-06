/*

    ngs-reads  Next generation sequencing (NGS/HTS) reads.
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
package org.nmdp.ngs.reads;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Flushable;
import java.io.IOException;

import java.util.Iterator;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;

import org.apache.commons.math3.random.RandomGenerator;

import org.biojava.bio.program.fastq.Fastq;
import org.biojava.bio.program.fastq.FastqBuilder;
import org.biojava.bio.program.fastq.FastqVariant;
import org.biojava.bio.program.fastq.FastqWriter;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;

import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;

/**
 * Generate paired-end next generation sequencing (NGS/HTS) reads.
 */
public final class GeneratePairedEndReads implements Runnable {
    /** Reference. */
    private final Sequence reference;

    /** FASTQ variant. */
    private final FastqVariant variant;

    /** Random. */
    private final RandomGenerator random;

    /** Location distribution. */
    private final IntegerDistribution location;

    /** Length distribution. */
    private final RealDistribution length;

    /** Insert size distribution. */
    private final RealDistribution insertSize;

    /** Quality strategy. */
    private final QualityStrategy quality;

    /** Coverage strategy. */
    private final CoverageStrategy coverage;

    /** Mutation rate. */
    private final double mutationRate;

    /** Mutation strategy. */
    private final MutationStrategy mutation;

    /** First appendable. */
    private final Appendable first;

    /** Second appendable. */
    private final Appendable second;

    /** FASTQ writer. */
    private final FastqWriter writer;


    /**
     * Generate paired-end next generation sequencing (NGS/HTS) reads.
     *
     * @param reference reference, must not be null
     * @param variant FASTQ variant, must not be null
     * @param random random, must not be null
     * @param length length distribution, must not be null
     * @param insertSize insert size distribution, must not be null
     * @param quality quality strategy, must not be null
     * @param coverage coverage strategy, must not be null
     * @param mutationRate mutation rate, must be between <code>0.0</code> and <code>1.0</code>, inclusive
     * @param mutation mutation strategy, must not be null
     * @param first first appendable, must not be null
     * @param second second appendable, must not be null
     * @param writer FASTQ writer, must not be null
     */
    public GeneratePairedEndReads(final Sequence reference,
                                  final FastqVariant variant,
                                  final RandomGenerator random,
                                  final RealDistribution length,
                                  final RealDistribution insertSize,
                                  final QualityStrategy quality,
                                  final CoverageStrategy coverage,
                                  final double mutationRate,
                                  final MutationStrategy mutation,
                                  final Appendable first,
                                  final Appendable second,
                                  final FastqWriter writer) {

        checkNotNull(reference, "reference must not be null");
        checkNotNull(variant, "variant must not be null");
        checkNotNull(random, "random must not be null");
        checkNotNull(length, "length must not be null");
        checkNotNull(insertSize, "insertSize must not be null");
        checkNotNull(quality, "quality must not be null");
        checkNotNull(coverage, "coverage must not be null");
        checkArgument(mutationRate >= 0.0d, "mutationRate must be greater than or equal to 0.0d");
        checkArgument(mutationRate <= 1.0d, "mutationRate must be less than or equal to 1.0d");
        checkNotNull(mutation, "mutation must not be null");
        checkNotNull(first, "first must not be null");
        checkNotNull(second, "second must not be null");
        checkNotNull(writer, "writer must not be null");

        this.reference = reference;
        this.variant = variant;
        this.random = random;
        this.length = length;
        this.insertSize = insertSize;
        this.quality = quality;
        this.coverage = coverage;
        this.mutationRate = mutationRate;
        this.mutation = mutation;
        this.first = first;
        this.second = second;
        this.writer = writer;

        int flanking = (int) (length.getNumericalMean() + length.getNumericalVariance() + insertSize.getNumericalMean() + insertSize.getNumericalVariance());
        location = new UniformIntegerDistribution(this.random, 1 - flanking, this.reference.length() + flanking);
    }


    @Override
    public void run() {
        int count = 0;

        // while coverage not met
        while (!coverage.evaluate(reference)) {
            // sample first location
            int firstStart = location.sample();
            int firstEnd = firstStart + (int) length.sample();

            // write if valid first location
            if (firstStart < reference.length() && firstEnd > 1) {
                write(firstStart, firstEnd, false, count, " 1", first);
            }

            // sample second location
            int secondStart = firstEnd + (int) insertSize.sample();
            int secondEnd = secondStart + (int) length.sample();

            // write if valid second location
            if (secondStart < reference.length() && secondEnd > 1) {
                write(secondStart, secondEnd, true, count, " 2", second);
            }
            count++;
        }
    }

    // todo:  refactor out the boolean parameter
    private void write(final int start, final int end, final boolean reverse, final int count, final String suffix, final Appendable appendable) {
        // truncate on both ends
        int s = Math.max(1, start);
        int e = Math.min(reference.length() + 1, end);
        SymbolList read = reference.subList(s, e - 1);

        // mutate and build sequence
        StringBuilder sequence = new StringBuilder();
        for (Iterator<Symbol> i = read.iterator(); i.hasNext(); ) {
            Symbol symbol = i.next();
            if (random.nextDouble() < mutationRate) {
                for (Iterator<Symbol> j = mutation.mutate(symbol).iterator(); j.hasNext(); ) {
                    Symbol mutated = j.next();
                    try {
                        sequence.append(DNATools.dnaToken(mutated));
                    }
                    catch (IllegalSymbolException ex) {
                        // ignore
                    }
                }
            }
            else {
                try {
                    sequence.append(DNATools.dnaToken(symbol));
                }
                catch (IllegalSymbolException ex) {
                    // ignore
                }
            }
        }

        if (reverse) {
            // reverse orientation
            sequence.reverse();

            // ...and reverse complement
            for (int index = 0; index < sequence.length(); index++) {
                if (sequence.charAt(index) == 'a') {
                    sequence.setCharAt(index, 't');
                }
                else if (sequence.charAt(index) == 't') {
                    sequence.setCharAt(index, 'a');
                }
                else if (sequence.charAt(index) == 'g') {
                    sequence.setCharAt(index, 'c');
                }
                else if (sequence.charAt(index) == 'c') {
                    sequence.setCharAt(index, 'g');
                }
            }
        }

        // apply quality scores
        StringBuilder qualityScores = new StringBuilder();
        for (int i = 0, size = sequence.length(); i < size; i++) {
            double qualityScore = quality.qualityScore(i, size);
            qualityScores.append(variant.quality(Math.max(Math.min((int) qualityScore, variant.maximumQualityScore()), variant.minimumQualityScore())));
        }

        // write fastq
        Fastq fastq = new FastqBuilder()
            .withVariant(variant)
            .withDescription(reference.getName() + "-" + count + suffix)
            .withSequence(sequence.toString())
            .withQuality(qualityScores.toString())
            .build();

        try {
            writer.append(appendable, fastq);

            if (appendable instanceof Flushable) {
                ((Flushable) appendable).flush();
            }
        }
        catch (IOException ex) {
            // ignore
        }

        // update coverage
        coverage.add(reference, s, Math.min(reference.length() + 1, s + read.length()));
    }
}
