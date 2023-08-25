/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.util;

public class ByteRange implements Range {
	private Long start;
	private Long end;

	public ByteRange() {
	}

	public ByteRange(Long start, Long end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * @return the start
	 */
	public Long getStart() {
		return start;
	}

	public long getStartOrZero() {
		return start != null ? (long) start : 0;
	}

	/**
	 * @param start the start to set
	 */
	public ByteRange setStart(Long start) {
		this.start = start;
		return this;
	}

	/**
	 * @return the end
	 */
	public Long getEnd() {
		return end;
	}

	/**
	 * @param end the end to set
	 */
	public ByteRange setEnd(Long end) {
		this.end = end;
		return this;
	}

	private void limitTime(ByteRange range) {
		if (range.start != null) {
			if (start != null) {
				start = Math.max(start, range.start);
			}
			if (end != null) {
				end = Math.max(end, range.start);
			}
		}
		if (range.end != null) {
			if (start != null) {
				start = Math.min(start, range.end);
			}
			if (end != null) {
				end = Math.min(end, range.end);
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ByteRange [start=" + start + ", end=" + end + "]";
	}

	@Override
	public void limit(Range range) {
		limitTime((ByteRange) range);
	}

	@Override
	public boolean isStartOffsetAvailable() {
		return start != null;
	}

	@Override
	public boolean isEndLimitAvailable() {
		return end != null;
	}

	@Override
	public double getDuration() {
		return 0;
	}

	@Override
	public ByteRange asByteRange() {
		return this;
	}

	@Override
	public TimeRange createTimeRange() {
		return new TimeRange(null, null);
	}
}
