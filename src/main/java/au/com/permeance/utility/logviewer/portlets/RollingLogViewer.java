/**
 * Copyright (C) 2013 Permeance Technologies
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */

package au.com.permeance.utility.logviewer.portlets;

/**
 * RollingLogViewer
 *
 * @author Chun Ho <chun.ho@permeance.com.au>
 */
public class RollingLogViewer {

	/**
	 * size of the character buffer array used to read and store logs
	 */
	public static final int CHAR_SIZE = 5 * 1024 * 1024;

	public RollingLogViewer() {
		intbuf = new char[CHAR_SIZE];
		pnt = 0;
	}

	public char[] getBuffer(final int oldpointerparam, final int newpointer) {
		int oldpointer = oldpointerparam;

		if (!bufwrapped && newpointer < oldpointer) {
			oldpointer = -1;
		}

		if (oldpointer == -1) {
			if (bufwrapped) {
				oldpointer =
					((newpointer - BACK_FILL_SIZE) + intbuf.length) % intbuf.length;
			} else {
				oldpointer = newpointer - BACK_FILL_SIZE;

				if (oldpointer < 0) {
					oldpointer = 0;
				}
			}
		}

		if (newpointer == oldpointer) {
			return new char[0];
		}

		if (newpointer > oldpointer) {
			final char[] toReturn = new char[newpointer - oldpointer];
			System.arraycopy(
				intbuf, oldpointer, toReturn, 0, newpointer - oldpointer);

			return toReturn;
		} else {

			// loop around

			final char[] toReturn =
				new char[intbuf.length - oldpointer + newpointer];
			final int offlength = intbuf.length - oldpointer;
			System.arraycopy(intbuf, oldpointer, toReturn, 0, offlength);
			System.arraycopy(intbuf, 0, toReturn, offlength, newpointer);

			return toReturn;
		}
	}

	public int getCurrentPointer() {
		return pnt;
	}

	public synchronized void write(
		final char[] buf, final int origoffset, final int origlength) {

		int offset = origoffset;
		int length = origlength;

		if (length > intbuf.length) {

			// there is going to be log truncation because the log volume in larger than the buffer

			offset = offset + (length - intbuf.length);
			length = intbuf.length;
		}

		if (pnt + length > intbuf.length) {
			final int offlength = intbuf.length - pnt;
			System.arraycopy(buf, offset, intbuf, pnt, offlength);
			System.arraycopy(
				buf, offset + offlength, intbuf, 0, length - offlength);
		} else {
			System.arraycopy(buf, offset, intbuf, pnt, length);
		}

		if (pnt + length >= intbuf.length) {
			bufwrapped = true;
		}

		pnt = (pnt + length) % intbuf.length;
	}

	/**
	 * number of characters of log to return at start
	 */
	private static final int BACK_FILL_SIZE = 1000;

	private boolean bufwrapped = false;
	private final char[] intbuf;
	private int pnt;

}