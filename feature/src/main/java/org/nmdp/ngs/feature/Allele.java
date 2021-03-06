/*

    ngs-feature  Features.
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
package org.nmdp.ngs.feature;

import java.util.Collections;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import org.biojava.bio.seq.DNATools;

import org.biojava.bio.symbol.Edit;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.bio.symbol.SymbolList;

import org.biojava.utils.ChangeVetoException;
/**
 * Allele class for extending assembly-specific genomic coordinates (Locus objects) with sequence data.
 */
public final class Allele extends Locus {
    /**
     * Name of the allele.
     */
    String name;
    /**
     * Enumeration of possible lesion types.
     */
    public enum Lesion {
        /**
         * Unknown or undetermined.
         */
        UNKNOWN,
        /**
         * Matches the specified reference sequence at the corresponding genomic location.
         */
        MATCH,
        /**
         * A single basepair mismatch with respect to the reference sequence.
         */
        SUBSTITUTION,
        /**
         * One or more gaps in the sequence with respect to the reference.
         */
        INSERTION,
        /**
         * One or more gaps in the reference with respect to the sequence.
         */
        DELETION
    }
    /**
     * Sequence of acceptable characters (alphabet) at the corresponding genomic location.
     */
    public final SymbolList sequence;
    /**
     * Lesion represented by the sequence with respect to a specified reference sequence. The reference sequence is assigned with proper Allele object instantiation using a Builder.
     */
    public final Lesion lesion;
    /**
     * Construct a new allele.
     * @param name of the allele
     * @param contig assembly (may include information about the source reference)
     * @param start or lower genomic coordinate
     * @param end or upper genomic coordinate
     * @param sequence of the allele
     * @param lesion or named difference with respect to the reference
     */
    Allele(final String name, final String contig, final int start, final int end, final SymbolList sequence, final Lesion lesion) {
        super(contig, start, end);
        this.name = name;
        this.sequence = sequence; 
        this.lesion = lesion;
    }
    /**
     * 
     * @return allele name 
     */
    public String getName() {
        return name;
    }
    /**
     * 
     * @param name assignment 
     */
    public void setName(final String name) {
        this.name = name;
    }
    /**
     * 
     * @return new parameterized Allele object
     */
    public static Builder builder() {
        return new Builder();
    }
    /**
     * Builder class for Allele.
     */
    public static final class Builder {
        private String name, contig, reference;
        private int start, end;
        private SymbolList sequence;
        private Lesion lesion;
        /**
         * Construct a new allele with unassigned members.
         */
        private Builder() {
            // empty
        }
        /**
         * 
         * @param name assignment
         * @return this builder with assigned name
         */
        public Builder withName(final String name) {
            this.name = name;
            return this;
        }
        /**
         * 
         * @param contig assignment
         * @return this builder with assigned contig
         */
        public Builder withContig(final String contig) {
            this.contig = contig;
            return this;
        }
        /**
         * 
         * @param start assignment
         * @return this builder with assigned start
         */
        public Builder withStart(final int start) {
            this.start = start;
            return this;
        }
        /**
         * 
         * @param end assignment
         * @return this builder with assigned end
         */
        public Builder withEnd(final int end) {
            this.end = end;
            return this;
        }
        /**
         * 
         * @param sequence assignment
         * @return this builder with assigned sequence
         */
        public Builder withSequence(final SymbolList sequence) {
            this.sequence = sequence;
            return this;
        }
        /**
         * 
         * @param lesion assignment
         * @return this builder with assigned lesion
         */
        public Builder withLesion(final Lesion lesion) {
            this.lesion = lesion;
            return this;
        }
        /**
         * 
         * @param reference assignment (a URI is nice, but not strictly enforced)
         * @return this builder with assigned reference
         */
        public Builder withReference(final String reference) {
            this.reference = reference;
            return this;
        }
        /**
         * 
         * @return reference identifier
         */
        public String getReference() {
            return reference;
        }
        /**
         * 
         * @return builder with Allele members set to default values
         */
        public Builder reset() {
            this.name = "";
            this.contig = "";
            this.reference = "";
            this.start = 0;
            this.end = 0;
            this.sequence = SymbolList.EMPTY_LIST;
            this.lesion = Lesion.UNKNOWN;
            return this;
        }
        /**
         * 
         * @return new Allele parameterized using with methods
         * @throws AlleleException if Allele cannot be built because the assigned sequence contains illegal symbols (eg non-nucleic acid)
         */
        public Allele build() throws AlleleException {

            if (sequence == null || sequence == SymbolList.EMPTY_LIST) {
                String gaps = Joiner.on("").join(Collections.nCopies(end - start, "-"));

                try {
                    this.sequence = DNATools.createDNA(gaps);
                }
                catch (IllegalSymbolException ex) {
                    throw new AlleleException("cannot build allele: " + ex.getMessage());
                }
            }

            if (reference != null && !reference.isEmpty()) {
                if (lesion == Lesion.INSERTION || lesion == Lesion.DELETION) {
                    if (end == start) {
                        start = Locus.Util.pushLeft(start, sequence.seqString(), reference);
                        end = Locus.Util.pushRight(end, sequence.seqString(), reference);
                    }
                }
            }

            return new Allele(name, contig, start, end, sequence, lesion);
        }
    }
    /**
     * Define allele equivalence as the same genomic location (contig, start, and end) as well as the same lesion and sequence.
     * @param right allele
     * @return true if this is equal to right, false otherwise
     */
    @Override
    public boolean equals(final Object right) {
        if (!super.equals(right)) {
            return false;
        }

        Allele allele = (Allele) right;
        return allele.lesion == this.lesion && allele.sequence.equals(this.sequence);
    }
    /**
     * 
     * @return allele hash code
     */
    @Override
    public int hashCode() {
      return super.hashCode() + Objects.hashCode(name, lesion) + sequence.hashCode();
    }
    /**
     * A method to simulate double crossover between Allele objects whereby the sequence from one allele is joined to the other within a specific region of overlap.
     * @param right allele
     * @return a new crossed-over allele. This implementation treats the original alleles as immutable thus favoring programmatic convenience over genetic reality.
     * @throws IllegalSymbolException 
     * @throws IndexOutOfBoundsException
     * @throws IllegalAlphabetException 
     */
    public Allele doubleCrossover(final Allele right) throws IllegalSymbolException, IndexOutOfBoundsException, IllegalAlphabetException {
        if (this.overlaps(right)) {
            //System.out.println("this range" + this.toString() + " sequence = " + this.sequence.seqString() + "sequence length = " + sequence.length());

            Locus homologue = intersection(right);

            //System.out.println("homologue = " + homologue);

            SymbolList copy = DNATools.createDNA(this.sequence.seqString());
            int length = homologue.getEnd() - homologue.getStart();
            int target = homologue.getStart() - right.getStart() + 1;
            int from = homologue.getStart() - this.getStart() + 1;

            //System.out.println("length = " + length);
            //System.out.println("target = " + target);
            //System.out.println("from = " + from);
            //System.out.println("copy = " + copy.seqString());

            try {
                SymbolList replace = right.sequence.subList(target, target + length - 1);
                //System.out.println("replace = " + replace.seqString());
                copy.edit(new Edit(from, length, replace));
            }
            catch(ChangeVetoException e) {
                //System.out.println("CHANGE VETO EXCEPTON" + e.getMessage());
            }

            //System.out.println("CROSSEDOVER SEQUENCE = " + copy.seqString());


            //copy.edit(new Edit());

            //Sequence left = this.sequence.subList(0, homologue.getStart());
            //Sequence middle = right.sequence.subList(homologue.getStart() - right.getStart(), i1);
            return new Allele(this.name, this.contig, this.getStart(), this.getEnd(), copy, Lesion.UNKNOWN);
        }
        return new Allele(this.name, this.contig, this.getStart(), this.getEnd(), this.sequence, Lesion.UNKNOWN);
    }
    /**
     * A method to simulate merging of two alleles strictly, meaning the sequence in the overlapping regions must be equal.
     * @param right allele
     * @param minimumOverlap for the merge to occur
     * @return new merged allele.
     * @throws IllegalSymbolException
     * @throws IndexOutOfBoundsException
     * @throws IllegalAlphabetException
     * @throws AlleleException 
     */
    public Allele merge(final Allele right, final long minimumOverlap) throws IllegalSymbolException, IndexOutOfBoundsException, IllegalAlphabetException, AlleleException {
      
        Allele.Builder builder = Allele.builder();
        Locus overlap = intersection(right);

        // System.out.println("overlap = " + overlap);
        // System.out.println("overlap.length() " + overlap.length() + " < " + startimumOverlap + "??");

        if (overlap.length() < minimumOverlap) {
            return builder.reset().build();
        }

        Allele bit = builder  
            .withContig(overlap.getContig())
            .withStart(overlap.getStart())
            .withEnd(overlap.getEnd())
            .withSequence(SymbolList.EMPTY_LIST)
            .withLesion(Lesion.UNKNOWN)
            .build();

        // System.out.println("bit = " + bit + " " + bit.sequence.seqString());

        Allele a = bit.doubleCrossover(right);

        // System.out.println("a = " + a + " " + a.sequence.seqString());

        Allele b = bit.doubleCrossover(this);

        // System.out.println("b = " + b + " " + b.sequence.seqString());   

        if (a.sequence.seqString().equals(b.sequence.seqString())) {
            Locus union = union(right);
            return builder
                .withName(right.getName())
                .withContig(union.getContig())
                .withStart(union.getStart())
                .withEnd(union.getEnd())
                .withSequence(SymbolList.EMPTY_LIST)
                .withLesion(Lesion.UNKNOWN)
                .build()

                .doubleCrossover(right)
                .doubleCrossover(this);
        }
        return builder.reset().build(); 
    }
    /**
     * A method to simulate hard clipping (removal) of leftmost sequence, for example synthesized sequence representing molecular barcodes or target capture probes (primers)
     * @param pattern sequence (will be strictly matched, no regular expressions)
     * @return new Allele with clipped sequence
     * @throws IllegalAlphabetException
     * @throws AlleleException
     * @throws IllegalSymbolException 
     */
    public Allele leftHardClip(final String pattern) throws IllegalAlphabetException, AlleleException, IllegalSymbolException {
        int start = this.getStart();

        SymbolList copy = DNATools.createDNA(sequence.seqString());
        while (copy.seqString().startsWith(pattern)) {
            copy.edit(new Edit(1, pattern.length(), SymbolList.EMPTY_LIST));
            start += pattern.length();
        }

        return builder()
            .withContig(this.getContig())
            .withStart(start)
            .withEnd(this.getEnd())
            .withSequence(copy)
            .withLesion(this.lesion)
            .build();
    }
    /**
     * A method to simulate hard clipping (removal) of rightmost sequence, for example synthesized sequence representing molecular barcodes or target capture probes (primers)
     * @param pattern sequence (will be strictly matched, no regular expressions)
     * @return new Allele with clipped sequence
     * @throws IllegalAlphabetException
     * @throws AlleleException
     * @throws IllegalSymbolException 
     */
    public Allele rightHardClip(final String pattern) throws IllegalSymbolException, IndexOutOfBoundsException, IllegalAlphabetException, AlleleException {
        int end = this.getEnd();

        // System.out.println("end before = " + end);

        SymbolList copy = DNATools.createDNA(sequence.seqString());
        while (copy.seqString().endsWith(pattern)) {
            copy.edit(new Edit(copy.length() - pattern.length() + 1, pattern.length(), SymbolList.EMPTY_LIST));

            end -= pattern.length();

            // System.out.println("end during " + end);
            // System.out.println("editing copy " + copy.seqString());
        }

        // System.out.println("end after = " + end);
        // System.out.println("edited copy = " + copy.seqString());

        return builder()
            .withContig(this.getContig())
            .withStart(this.getStart())
            .withEnd(end)
            .withSequence(copy)
            .withLesion(this.lesion)
            .build();
    }
}
