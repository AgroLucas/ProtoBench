package com.agrolucas.persistence;

import com.agrolucas.model.Capture;
import com.agrolucas.model.MessageType;

import java.util.List;

/**
 * Everything a saved project holds. Plain data, handed between the UI state and {@link ProjectStorage}.
 *
 * @param captures, the captured packets
 * @param messageTypes, the message types and their fields
 * @param referenceIndex, index into captures of the reference capture, -1 when there is none
 * @param viewedMessageTypeIndex, index into messageTypes of the one being shown, -1 when there is none
 */
public record ProjectData(List<Capture> captures,
                          List<MessageType> messageTypes,
                          int referenceIndex,
                          int viewedMessageTypeIndex) {
}
