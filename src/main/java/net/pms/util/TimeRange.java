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

public class TimeRange implements Range {
	private Double start;
	private Double end;

	public TimeRange() {
	}

	public TimeRange(Double start, Double end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * @return the start
	 */
	public Double getStart() {
		return start;
	}

	public double getStartOrZero() {
		return start != null ? (double) start : 0;
	}

	/**
	 * @param start the start to set
	 */
	public TimeRange setStart(Double start) {
		this.start = start;
		return this;
	}

	/**
	 * Move the start position by amount, if the start position exists.
	 * @param amount
	 */
	public void rewindStart(double amount) {
		if (this.start != null) {
			if (this.start > amount) {
				this.start -= amount;
			} else {
				this.start = 0d;
			}
		}
	}

	/**
	 * @return the end
	 */
	public Double getEnd() {
		return end;
	}

	public double getEndOrZero() {
		return end != null ? (double) end : 0;
	}

	/**
	 * @param end the end to set
	 */
	public TimeRange setEnd(Double end) {
		this.end = end;
		return this;
	}

	public ByteRange createScaledRange(long scale) {
		return new ByteRange(start != null ? (long) (scale * start) : null, end != null ? (long) (scale * end) : null);
	}

	private void limitTime(TimeRange range) {
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
		return "TimeRange [start=" + start + ", end=" + end + "]";
	}

	@Override
	public void limit(Range range) {
		limitTime((TimeRange) range);
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
		return isStartOffsetAvailable() ? end - start : getEndOrZero();
	}

	@Override
	public ByteRange asByteRange() {
		throw new RuntimeException("Unable to convert to ByteRange:" + this);
	}

	@Override
	public TimeRange createTimeRange() {
		return new TimeRange(start, end);
	}

}