package fr.loria.score;

import fr.loria.score.jupiter.tree.Tree;
import fr.loria.score.jupiter.tree.TreeFactory;

/**
 * A little DSL to simplify test writing.
 *
 * @author Gerald.Oster@loria.fr
 */
public class TreeDSL
{
    private Tree wrappedTree;

    public TreeDSL(Tree t)
    {
        this.wrappedTree = t;
    }

    public TreeDSL addChild(TreeDSL... children)
    {
        for (TreeDSL c : children) {
            this.wrappedTree.addChild(c.wrappedTree);
        }
        return this;
    }

    public void removeChild(int i)
    {
        this.wrappedTree.removeChild(i);
    }

    public TreeDSL getChild(int i)
    {
        return new TreeDSL(this.wrappedTree.getChild(i));
    }

    public void removeChild()
    {
        int childCount = this.wrappedTree.nbChildren();
        for (int i = 0; i < childCount; i++) {
            this.wrappedTree.removeChild(0);
        }
    }

    public void clear()
    {
        removeChild();
    }

    public TreeDSL setAttribute(String styleName, String styleValue)
    {
        if (!this.wrappedTree.getNodeName().equals("span")) {
            throw new UnsupportedOperationException("Not supported by this DSL");
        }
        this.wrappedTree.setAttribute(styleName, styleValue);
        return this;
    }

    public static TreeDSL paragraph()
    {
        return new TreeDSL(TreeFactory.createParagraphTree());
    }

    public static TreeDSL text(String str)
    {
        return new TreeDSL(TreeFactory.createTextTree(str));
    }

    public static TreeDSL span(String styleName, String styleValue)
    {
        Tree span = TreeFactory.createElementTree("span");
        span.setAttribute(styleName, styleValue);
        return new TreeDSL(span);
    }
       
    public static TreeDSL hr()
    {
        return new TreeDSL(TreeFactory.createHorizontalRuleTree());
    }

    public Tree getTree()
    {
        return wrappedTree;
    }

    @Override public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if (obj instanceof TreeDSL) {
            TreeDSL other = (TreeDSL) obj;
            return wrappedTree.equals(other.getTree());
        }
        return false;
    }
}
