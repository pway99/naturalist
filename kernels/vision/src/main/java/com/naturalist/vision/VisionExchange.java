package com.naturalist.vision;

/**
 * A tool-use conversation with the vision model. {@link #result()} is the tool call
 * from the latest turn; {@link #respond} sends a tool result back and returns the next
 * turn. Lets the caller run a second turn (e.g. hand the model similar existing
 * features and get a reuse-or-new resolution) without the kernel knowing the
 * provider's conversation mechanics.
 */
public interface VisionExchange {

    ToolResult result();

    /** Send {@code toolResultJson} as the tool result for {@link #result()} and get the next turn, forcing {@code nextTool}. */
    VisionExchange respond(String toolResultJson, ToolSchema nextTool);
}
