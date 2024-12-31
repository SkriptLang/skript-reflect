package com.btk5h.skriptmirror.util;

import java.util.Arrays;

public class StringSimilarity {

	public static class Result implements Comparable<Result> {
		private final String left;
		private final String right;
		private final int editDistance;

		private Result(String left, String right, int editDistance) {
			this.left = left;
			this.right = right;
			this.editDistance = editDistance;
		}

		public String getLeft() {
			return left;
		}

		public String getRight() {
			return right;
		}

		public int getEditDistance() {
			return editDistance;
		}


		@Override
		public int compareTo(Result o) {
			return getEditDistance() - o.getEditDistance();
		}
	}

	/**
	 * Calculates Levenshtein distance between two strings.
	 * <br>
	 * Source: <a href="https://github.com/apache/commons-text/blob/a7045b5df428284762f2bb9cc4e22422f33d0d73/src/main/java/org/apache/commons/text/similarity/LevenshteinDistance.java#L166">Apache</a>
	 * <br>
	 * See <a href="https://github.com/apache/commons-text/blob/a7045b5df428284762f2bb9cc4e22422f33d0d73/LICENSE.txt">LICENSE.TXT</a> for the license.
	 */
	public static Result compare(String left, String right, int threshold) {
		String first = left;
		String second = right;
		if (first == null || second == null) {
			throw new IllegalArgumentException("CharSequences must not be null");
		}
		if (threshold < 0) {
			throw new IllegalArgumentException("Threshold must not be negative");
		}

		int n = first.length(); // length of left
		int m = second.length(); // length of right

		// if one string is empty, the edit distance is necessarily the length
		// of the other
		if (n == 0) {
			return m <= threshold ? new Result(left, right, m) : null;
		} else if (m == 0) {
			return n <= threshold ? new Result(left, right, n) : null;
		}

		if (n > m) {
			// swap the two strings to consume less memory
			final String tmp = first;
			first = second;
			second = tmp;
			n = m;
			m = second.length();
		}

		// the edit distance cannot be less than the length difference
		if (m - n > threshold) {
			return null;
		}

		int[] p = new int[n + 1]; // 'previous' cost array, horizontally
		int[] d = new int[n + 1]; // cost array, horizontally
		int[] tempD; // placeholder to assist in swapping p and d

		// fill in starting table values
		final int boundary = Math.min(n, threshold) + 1;
		for (int i = 0; i < boundary; i++) {
			p[i] = i;
		}
		// these fills ensure that the value above the rightmost entry of our
		// stripe will be ignored in following loop iterations
		Arrays.fill(p, boundary, p.length, Integer.MAX_VALUE);
		Arrays.fill(d, Integer.MAX_VALUE);

		// iterates through t
		for (int j = 1; j <= m; j++) {
			final char secondJ = second.charAt(j - 1); // jth character of second
			d[0] = j;

			// compute stripe indices, constrain to array size
			final int min = Math.max(1, j - threshold);
			final int max = j > Integer.MAX_VALUE - threshold ? n : Math.min(
				n, j + threshold);

			// ignore entry left of leftmost
			if (min > 1) {
				d[min - 1] = Integer.MAX_VALUE;
			}

			// iterates through [min, max] in s
			for (int i = min; i <= max; i++) {
				if (first.charAt(i - 1) == secondJ) {
					// diagonally left and up
					d[i] = p[i - 1];
				} else {
					// 1 + minimum of cell to the left, to the top, diagonally
					// left and up
					d[i] = 1 + Math.min(Math.min(d[i - 1], p[i]), p[i - 1]);
				}
			}

			// copy current distance counts to 'previous row' distance counts
			tempD = p;
			p = d;
			d = tempD;
		}

		// if p[n] is greater than the threshold, there's no guarantee on it
		// being the correct
		// distance
		if (p[n] <= threshold) {
			return new Result(left, right, p[n]);
		}
		return null;
	}

}
