package org.doomdark.uuid;

import java.util.Random;

/* JUG Java Uuid Generator
 *
 * Copyright (c) 2002 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * You can redistribute this work and/or modify it under the terms of
 * LGPL (Lesser General Public License), as published by
 * Free Software Foundation (http://www.fsf.org). No warranty is
 * implied. See LICENSE for details about licensing.
 */

/**
 * UUIDTimer produces the time stamps required for time-based UUIDs.
 * It works according to UUID-specification, with following
 * modifications:
 *
 *<ul>
 *<li>Java libs can only product time stamps with maximum resolution
 *   of one millisecond. To compensate, an additional counter is used,
 *   so that more than one UUID can be generated between java clock
 *   updates. Counter may be used to generate up to 10000 UUIDs for
 *   each distrinct java clock value (which in some cases may mean
 *   getting 10k UUIDs for each 55 msecs, a la Windows, ie only
 *   about 200000 UUIDs per second).
 *<li> An additional precaution, counter is initialized not to 0
 *   but to a random 8-bit number, and each time clock changes, lowest
 *   8-bits of counter are preserved. The purpose it to make likelyhood
 *   of multi-JVM multi-instance generators to collide, without significantly
 *   reducing max. UUID generation speed. Note though that using more than
 *   one generator (from separate JVMs) is strongly discouraged, so
 *   hopefully this enhancement isn't needed.
 *   This 8-bit offset has to be reduced from total max. UUID count to
 *   preserve ordering property of UUIDs (ie. one can see which UUID
 *   was generated first for given UUID generator); the resulting
 *   9500 UUIDs isn't much different from the optimal choice.
 *</ul>
 *<p>
 *Some additional assumptions about calculating the timestamp:
 *<ul>
 *<li>System.currentTimeMillis() is assumed to give time offset in UTC,
 *   or at least close enough thing to get correct timestamps. The
 *   alternate route would have to go through calendar object, use
 *   TimeZone offset to get to UTC, and then modify. Using currentTimeMillis
 *   should be much faster to allow rapid UUID creation.
 *<li>Similarly, the constant used for time offset between 1.1.1970 and
 *  start of Gregorian calendar is assumed to be correct (which seems
 *  to be the case when testing with Java calendars).
 *</ul>
 */
public class UUIDTimer
{
    /**
     * Since System.longTimeMillis() returns time from january 1st 1970,
     * and UUIDs need time from the beginning of gregorian calendar
     * (15-oct-1582), need to apply the offset:
     */
    private final static long kClockOffset = 0x01b21dd213814000L;
    /**
     * Also, instead of getting time in units of 100nsecs, we get something
     * with max resolution of 1 msec... and need the multiplier as well
     */
    private final static long kClockMultiplier = 10000;
    private final static long kClockMultiplierL = 10000L;

    private final Random mRnd;
    private final byte[] mClockSequence = new byte[2];

    private long mLastTimeStamp = 0L;
    private int mClockCounter = 0;

    public UUIDTimer(Random rnd)
    {
        mRnd = rnd;
        initTimeStamps();
    }

    private void initTimeStamps()
    {
	/* Let's also generate the clock sequence now, plus initialize
	 * low 8-bits of the internal clock counter (which is used to
	 * compensate for lacking accurace; instead of 100nsecs resolution
	 * is at best 1 msec, ie. 10000 coarser... so we have to use
	 * a counter)
	 */
	mRnd.nextBytes(mClockSequence);
	/* Counter is used to further make it slightly less likely that
	 * two instances of UUIDGenerator (from separate JVMs as no more
	 * than one can be created in one JVM) would produce colliding
	 * time-based UUIDs. The practice of using multiple generators,
	 * is strongly discouraged, of course, but just in case...
	 */
	byte[] tmp = new byte[1];
	mRnd.nextBytes(tmp);
	mClockCounter = tmp[0] & 0xFF;
	mLastTimeStamp = 0L;
    }

    public void getTimestamp(byte[] uuidData)
    {
	// First the clock sequence:
	uuidData[UUID.INDEX_CLOCK_SEQUENCE] = mClockSequence[0];
	uuidData[UUID.INDEX_CLOCK_SEQUENCE+1] = mClockSequence[1];

	long now = System.currentTimeMillis();

	/* Unless time goes backwards (ie. bug in currentTimeMillis()),
	 * this should never happen. Nonetheless, UUID draft states that
	 * if it does happen, clock sequence has to be re-randomized.
	 * Fair enough.
	 */
	if (now < mLastTimeStamp) {
	    initTimeStamps();
	} else if (now == mLastTimeStamp) {
	    // Ran 'out of timestamps' for this clock cycle? Have to wait!
	    if (mClockCounter == kClockMultiplier) {
		// Should this be randomised now?
		mClockCounter &= 0xFF;
		do {
		    try {
			Thread.sleep(1L);
		    } catch (InterruptedException ie) {
		    }
		    now = System.currentTimeMillis();
		} while (now == mLastTimeStamp);
		mLastTimeStamp = now;
	    }
	} else {
	    mClockCounter &= 0xFF;
	    mLastTimeStamp = now;
	}

	/* Now, let's translate the timestamp to one UUID needs, 100ns
	 * unit offset from the beginning of Gregorian calendar...
	 */
	now *= kClockMultiplierL;
	now += kClockOffset;

	// Plus add the clock counter:
	now += mClockCounter;

	/* Time fields are nicely split across the UUID, so can't just
	 * linearly dump the stamp:
	 */
	int clockHi = (int) (now >>> 32);
	int clockLo = (int) now;

	uuidData[UUID.INDEX_CLOCK_HI] = (byte) (clockHi >>> 24);
	uuidData[UUID.INDEX_CLOCK_HI+1] = (byte) (clockHi >>> 16);
	uuidData[UUID.INDEX_CLOCK_MID] = (byte) (clockHi >>> 8);
	uuidData[UUID.INDEX_CLOCK_MID+1] = (byte) clockHi;

	uuidData[UUID.INDEX_CLOCK_LO] = (byte) (clockLo >>> 24);
	uuidData[UUID.INDEX_CLOCK_LO+1] = (byte) (clockLo >>> 16);
	uuidData[UUID.INDEX_CLOCK_LO+2] = (byte) (clockLo >>> 8);
	uuidData[UUID.INDEX_CLOCK_LO+3] = (byte) clockLo;
	
	++mClockCounter;
    }
}
