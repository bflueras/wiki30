package fr.loria.score.jupiter.tree.operation;

import fr.loria.score.jupiter.tree.Tree;

public class TreeDeleteTree extends TreeOperation {

    public TreeDeleteTree() {}

    public TreeDeleteTree(int[] path) {
        setPath(path);
    }

    public void execute(Tree root) {
        Tree tree = root.getChildFromPath(path);
        tree.hide();
    }

    public String toString() {
        StringBuffer tab = new StringBuffer();
        for (int i = 0; i < path.length; i++) {
            tab.append("/").append(path[i]);
        }
        return "DeleteTree(" + super.toString() + tab + ")";
    }

    public TreeOperation transform(TreeOperation op1) {
        return transp(op1);
    }

    //OT pour DeleteTree
    private TreeOperation transp(TreeOperation op1) {
        return op1;
    }

    @Override
    protected TreeOperation handleTreeInsertText(TreeInsertText op1) {
        return null;
    }

    @Override
    protected TreeOperation handleTreeDeleteText(TreeDeleteText op1) {
        return null;
    }

    @Override
    protected TreeOperation handleTreeNewParagraph(TreeNewParagraph op1) {
        return null;
    }

    @Override
    protected TreeOperation handleTreeMergeParagraph(TreeMergeParagraph op1) {
        return null;
    }

    @Override
    protected TreeOperation handleTreeInsertParagraph(TreeInsertParagraph op1) {
        return null;
    }

    @Override
    protected TreeOperation handleTreeDeleteTree(TreeDeleteTree op1) {
        return null;
    }

    @Override
    protected TreeOperation handleTreeStyle(TreeStyle op1) {
        return null;
    }

    @Override
    protected TreeOperation handleTreeMoveParagraph(TreeMoveParagraph op1) {
        return null;
    }
}