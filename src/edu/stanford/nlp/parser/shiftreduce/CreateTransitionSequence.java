package edu.stanford.nlp.parser.shiftreduce;

import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.Generics;

public class CreateTransitionSequence {
  // static methods only.  
  // we could change this if we wanted to include options.
  private CreateTransitionSequence() {}

  public static List<Transition> createTransitionSequence(Tree tree, boolean compoundUnary) {
    List<Transition> transitions = Generics.newArrayList();

    createTransitionSequenceHelper(transitions, tree, compoundUnary);
    transitions.add(new FinalizeTransition());
    transitions.add(new IdleTransition());

    return transitions;
  }

  private static void createTransitionSequenceHelper(List<Transition> transitions, Tree tree, boolean compoundUnary) {
    if (tree.isLeaf()) {
      // do nothing
    } else if (tree.isPreTerminal()) {
      transitions.add(new ShiftTransition());
    } else if (tree.children().length == 1) {
      if (compoundUnary) {
        List<String> labels = Generics.newArrayList();
        while (tree.children().length == 1 && !tree.isPreTerminal()) {
          labels.add(tree.label().value());
          tree = tree.children()[0];
        }
        createTransitionSequenceHelper(transitions, tree, compoundUnary);
        transitions.add(new CompoundUnaryTransition(labels));
      } else {
        createTransitionSequenceHelper(transitions, tree.children()[0], compoundUnary);
        transitions.add(new UnaryTransition(tree.label().value()));
      }
    } else if (tree.children().length == 2) {
      createTransitionSequenceHelper(transitions, tree.children()[0], compoundUnary);
      createTransitionSequenceHelper(transitions, tree.children()[1], compoundUnary);

      // This is the tricky part... need to decide if the binary
      // transition is a left or right transition.  This is done by
      // looking at the existing heads of this node and its two
      // children.  The expectation is that the tree already has heads
      // assigned; otherwise, exception is thrown
      if (!(tree.label() instanceof CoreLabel) || 
          !(tree.children()[0].label() instanceof CoreLabel) ||
          !(tree.children()[1].label() instanceof CoreLabel)) {
        throw new IllegalArgumentException("Expected tree labels to be CoreLabel");
      }
      CoreLabel label = (CoreLabel) tree.label();
      CoreLabel leftLabel = (CoreLabel) tree.children()[0].label();
      CoreLabel rightLabel = (CoreLabel) tree.children()[1].label();
      Tree head = label.get(TreeCoreAnnotations.HeadWordAnnotation.class);
      Tree leftHead = leftLabel.get(TreeCoreAnnotations.HeadWordAnnotation.class);
      Tree rightHead = rightLabel.get(TreeCoreAnnotations.HeadWordAnnotation.class);
      if (head == null || leftHead == null || rightHead == null) {
        throw new IllegalArgumentException("Expected tree labels to have their heads assigned");
      }
      if (head == leftHead) {
        transitions.add(new BinaryTransition(tree.label().value(), BinaryTransition.Side.LEFT));
      } else if (head == rightHead) {
        transitions.add(new BinaryTransition(tree.label().value(), BinaryTransition.Side.RIGHT));
      } else {
        throw new IllegalArgumentException("Heads were incorrectly assigned: tree's head is not matched to either the right or left head");
      }
    } else {
      throw new IllegalArgumentException("Expected a binarized tree");
    }
  }
}
