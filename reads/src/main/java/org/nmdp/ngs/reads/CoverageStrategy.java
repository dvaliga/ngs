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

import org.biojava.bio.seq.Sequence;

/**
 * Coverage strategy.
 */
public interface CoverageStrategy {

    /**
     * Return true if the specified reference sequence has enough coverage.
     *
     * @param reference reference sequence to evaluate
     * @return true if the specified reference sequence has enough coverage
     */
    boolean evaluate(Sequence reference);

    /**
     * Add coverage for the specified reference sequence for the region defined by <code>[start, end]</code> inclusive.
     *
     * @param reference reference sequence
     * @param start region start, 1-based coordinate
     * @param end region end, 1-based coordinate
     */
    void add(Sequence reference, int start, int end);
}
