package fr.loria.score.jupiter.tree;

import fr.loria.score.jupiter.model.AbstractOperation;
import fr.loria.score.jupiter.model.Document;
import fr.loria.score.jupiter.tree.operation.TreeOperation;

import java.util.logging.Logger;

/**
 * Wrapper over a hierarchical tree model
 * @author Bogdan.Flueras@inria.fr
 */
public class TreeDocument implements Document {
    private transient static final Logger log = Logger.getLogger(TreeDocument.class.getName());
    private transient Tree root;

    public TreeDocument() {
    }

    public TreeDocument(Tree root) {
        this.root = root;
    }

    @Override
    public String getContent() {
        return root.toString();
    }

    @Override
    public void setContent(String content) {
        root.setValue(content);
    }

    @Override
    public void apply(AbstractOperation op) {
        log.fine("Apply: " + op + " to document: " + root.getValue());
        if (op instanceof TreeOperation) {
            TreeOperation treeOperation = (TreeOperation) op;
            treeOperation.execute(root);
        }
    }

    @Override
    public String toString() {
        return getContent();
    }
}