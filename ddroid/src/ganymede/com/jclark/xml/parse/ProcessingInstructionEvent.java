package com.jclark.xml.parse;

/**
 * Information about a processing instruction.
 * @see com.jclark.xml.parse.base.Application#processingInstruction
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:02 $
 */
public interface ProcessingInstructionEvent extends LocatedEvent {
  /**
   * Returns the target of the processing instruction.
   */
  String getName();
  /**
   * Returns the part of the processing instruction following the
   * target.  Leading white space is not included.
   * The string will be empty rather than null if the processing
   * instruction contains only a target.
   */
  String getInstruction();
}
