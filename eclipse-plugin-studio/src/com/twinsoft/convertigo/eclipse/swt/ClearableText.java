package com.twinsoft.convertigo.eclipse.swt;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class ClearableText extends Composite {
    private final Text text;
    private final Label clear;
    private final int clearSize;

    public ClearableText(Composite parent, int style) {
        super(parent, SWT.NONE);
        int dpi = getDisplay().getDPI().x;
        this.clearSize = Math.max(12, Math.round(12f * dpi / 96f));

        FormLayout layout = new FormLayout();
        layout.marginWidth = layout.marginHeight = 0;
        setLayout(layout);

        text = new Text(this, style | SWT.SINGLE);
        FormData textFD = new FormData();
        textFD.left = new FormAttachment(0);
        textFD.right = new FormAttachment(100, -(clearSize + 6));
        textFD.top = new FormAttachment(0);
        textFD.bottom = new FormAttachment(100);
        text.setLayoutData(textFD);

        clear = new Label(this, SWT.NONE);
        clear.setToolTipText("Clear");
        clear.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        clear.setAlignment(SWT.CENTER);
        clear.addListener(SWT.Paint, e -> drawX(e.gc, new Rectangle(0, 0, clear.getSize().x, clear.getSize().y)));

        FormData clearFD = new FormData(clearSize + 4, clearSize + 4);
        clearFD.right = new FormAttachment(100, -2);
        clearFD.top = new FormAttachment(50, -(clearSize + 4) / 2);
        clear.setLayoutData(clearFD);

        clear.addListener(SWT.MouseDown, e -> {
            if (text.getCharCount() > 0) text.setText("");
            text.setFocus();
        });

        text.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.character == SWT.ESC) {
                    e.doit = false;
                    if (text.getCharCount() > 0) text.setText("");
                }
            }
        });

        ModifyListener update = e -> refresh();
        text.addModifyListener(update);
        text.addListener(SWT.FocusIn,  e -> refresh());
        text.addListener(SWT.FocusOut, e -> refresh());
        addListener(SWT.Resize, e -> redraw());

        refresh();
    }

    private void drawX(GC gc, Rectangle a) {
        Color bg = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        gc.setBackground(bg);
        gc.fillRectangle(a);
        gc.setAntialias(SWT.ON);
        gc.setAlpha(180);
        gc.setLineWidth(1);
        int d = Math.min(a.width, a.height) - 2;
        int x = a.x + (a.width - d) / 2;
        int y = a.y + (a.height - d) / 2;
        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        gc.drawOval(x, y, d, d);
        int p = Math.max(3, d / 4);
        gc.setAlpha(220);
        gc.setLineWidth(2);
        gc.drawLine(x + p, y + p, x + d - p, y + d - p);
        gc.drawLine(x + d - p, y + p, x + p, y + d - p);
    }

    private void refresh() {
        boolean show = isEnabled() && text.isEnabled() && text.getCharCount() > 0;
        clear.setVisible(show);
        clear.setEnabled(show);
        FormData fd = (FormData) text.getLayoutData();
        int newRight = show ? -(clearSize + 6) : 0;
        if (fd.right.offset != newRight) {
            fd.right = new FormAttachment(100, newRight);
            layout();
        }
    }

    public Text getTextControl() { return text; }
    public void setMessage(String message) { text.setMessage(message); }
    public String getText() { return text.getText(); }
    public void setText(String value) { text.setText(value); }
    public void addModifyListener(ModifyListener l) { text.addModifyListener(l); }
    public void removeModifyListener(ModifyListener l) { text.removeModifyListener(l); }
    public void addKeyListener(KeyListener l) { text.addKeyListener(l); }
    public void setEditable(boolean editable) { text.setEditable(editable); }

    @Override public void notifyListeners(int eventType, Event event) {
        if (eventType == SWT.Modify) {
            Event e = event == null ? new Event() : event;
            if (e.widget == null || e.widget == this) {
                e.widget = text;
            }
            text.notifyListeners(eventType, e);
            return;
        }
        super.notifyListeners(eventType, event);
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        text.setEnabled(enabled);
        refresh();
    }
}
