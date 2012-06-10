package net.pms.dlna;

public abstract class Range implements Cloneable {

	public abstract void limit(Range range);

	public abstract boolean isByteRange();

	public boolean isTimeRange() {
		return !isByteRange();
	}

	public abstract boolean isStartOffsetAvailable();

	public abstract boolean isEndLimitAvailable();

	public double getDuration() {
		return 0;
	}

	public Byte asByteRange() {
		throw new RuntimeException("Unable to convert to ByteRange:" + this);
	}

	/**
	 * @return a Range.Time object, which is bounded if this is already a bounded Range.Time object
	 */
	public Time createTimeRange() {
		return new Time(null, null);
	}

	public static Range create(long lowRange, long highRange, Double timeseek, Double timeRangeEnd) {
		if (lowRange > 0 || highRange > 0) {
			return new Range.Byte(lowRange, highRange);
		}
		return new Range.Time(timeseek, timeRangeEnd);
	}

	public static class Time extends Range implements Cloneable {
		private Double start;
		private Double end;

		public Time() {
		}

		public Time(Double start, Double end) {
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
			return start != null ? start : 0;
		}

		/**
		 * @param start the start to set
		 */
		public Time setStart(Double start) {
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
					this.start = this.start - amount;
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
			return end != null ? end : 0;
		}

		/**
		 * @param end the end to set
		 */
		public Time setEnd(Double end) {
			this.end = end;
			return this;
		}

		@Override
		public void limit(Range range) {
			limitTime((Time) range);
		}

		@Override
		public boolean isByteRange() {
			return false;
		}

		private void limitTime(Time range) {
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
		public boolean isStartOffsetAvailable() {
			return start != null;
		}

		@Override
		public boolean isEndLimitAvailable() {
			return end != null;
		}

		@Override
		public double getDuration() {
			return start != null ? end - start : (end != null ? end : 0);
		}

		@Override
		public Time createTimeRange() {
			return new Time(start, end);
		}

		public Byte createScaledRange(long scale) {
			return new Byte(start != null ? (long) (scale * start) : null, end != null ? (long) (scale * end) : null);
		}
	}

	public static class Byte extends Range implements Cloneable {
		private Long start;
		private Long end;

		public Byte() {
		}

		public Byte(Long start, Long end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public void limit(Range range) {
			limitTime((Byte) range);
		}

		@Override
		public boolean isByteRange() {
			return true;
		}

		/**
		 * @return the start
		 */
		public Long getStart() {
			return start;
		}

		/**
		 * @param start the start to set
		 */
		public Byte setStart(Long start) {
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
		public Byte setEnd(Long end) {
			this.end = end;
			return this;
		}

		private void limitTime(Byte range) {
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
		public boolean isStartOffsetAvailable() {
			return start != null;
		}

		@Override
		public boolean isEndLimitAvailable() {
			return end != null;
		}

		@Override
		public Byte asByteRange() {
			return this;
		}

	}

}
