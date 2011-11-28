package fr.loria.score.jupiter.tree.operation;

import fr.loria.score.jupiter.tree.Tree;
import fr.loria.score.jupiter.tree.TreeUtils;

import java.util.Arrays;

public class TreeDeleteText extends TreeOperation {

    public TreeDeleteText() {}

    public TreeDeleteText(int position, int[] path) {
        super(position);
        setPath(path);
    }

    public void execute(Tree root) {
        Tree tree = root.getChildFromPath(path);
        tree.deleteChar(position);
    }

    public String toString(boolean withTicket) {
        String tab = Arrays.asList(path).toString().replace(',', '/');
        return "DeleteText(" + super.toString() + "," + tab + ")";
    }


    //OT pour DeleteText
    protected TreeOperation handleTreeInsertText(TreeInsertText op1) {
        if (TreeUtils.diff(op1.path, path)) {
            return op1;
        }
        if (op1.getPosition() <= position) {
            return op1;
        }
        return new TreeInsertText(op1.getSiteId(), op1.getPosition() - 1, op1.path, op1.text);
    }

    protected TreeOperation handleTreeDeleteText(TreeDeleteText op1) {
        if (TreeUtils.diff(op1.path, path)) {
            return op1;
        }
        if (op1.getPosition() == position) {
            return new TreeIdOp();
        }
        if (op1.getPosition() < position) {
            return op1;
        }
        return new TreeDeleteText(op1.getPosition() - 1, op1.path);
    }

    protected TreeOperation handleTreeNewParagraph(TreeNewParagraph op1) {
        return op1;
    }

    protected TreeOperation handleTreeMergeParagraph(TreeMergeParagraph op1) {
        return op1;
    }

    protected TreeOperation handleTreeInsertParagraph(TreeInsertParagraph op1) {
        if (TreeUtils.diff(op1.path, path)) {
            return op1;
        }
        if (op1.getPosition() <= position) {
            return op1;
        }
        return new TreeInsertParagraph(op1.getSiteId(), op1.getPosition() - 1, op1.path, op1.splitLeft);
    }

    protected TreeOperation handleTreeDeleteTree(TreeDeleteTree op1) {
        return op1;
    }

    protected TreeOperation handleTreeStyle(TreeStyle op1) {
        if (TreeUtils.diff(op1.path, path)) {
            return op1;
        }
        if (op1.start > position) {
            return new TreeStyle(op1.getSiteId(), op1.path, op1.start - 1, op1.end - 1, op1.param, op1.value, op1.addStyle, op1.splitLeft, op1.sr);
        }
        if (op1.end <= position) {
            return op1;
        }
        return new TreeStyle(op1.getSiteId(), op1.path, op1.start, op1.end - 1, op1.param, op1.value, op1.addStyle, op1.splitLeft, op1.sr);
    }

    protected TreeOperation handleTreeMoveParagraph(TreeMoveParagraph op1) {
        return op1;
    }
}