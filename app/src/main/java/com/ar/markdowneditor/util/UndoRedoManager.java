package com.ar.markdowneditor.util;

import java.util.ArrayDeque;
import java.util.Deque;

public class UndoRedoManager {

    private static final int MAX_STEPS = 50;

    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();

    private String current = "";
    private boolean suppress = false;

    public void init(String initialText) {
        current = initialText;
        undoStack.clear();
        redoStack.clear();
    }

    public void push(String newText) {
        if (suppress) return;
        if (newText.equals(current)) return;

        undoStack.push(current);
        if (undoStack.size() > MAX_STEPS) {
            undoStack.removeLast();
        }
        current = newText;
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    public String undo() {
        if (undoStack.isEmpty()) return null;
        redoStack.push(current);
        current = undoStack.pop();
        return current;
    }

    public String redo() {
        if (redoStack.isEmpty()) return null;
        undoStack.push(current);
        current = redoStack.pop();
        return current;
    }

    public void setSuppress(boolean suppress) {
        this.suppress = suppress;
    }
}
