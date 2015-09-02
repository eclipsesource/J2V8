// Copyright 2013 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef V8_COMPILER_NODE_PROPERTIES_H_
#define V8_COMPILER_NODE_PROPERTIES_H_

#include "src/compiler/node.h"
#include "src/types.h"

namespace v8 {
namespace internal {
namespace compiler {

class Operator;

// A facade that simplifies access to the different kinds of inputs to a node.
class NodeProperties FINAL {
 public:
  // ---------------------------------------------------------------------------
  // Input layout.
  // Inputs are always arranged in order as follows:
  //     0 [ values, context, frame state, effects, control ] node->InputCount()

  static int FirstValueIndex(Node* node) { return 0; }
  static int FirstContextIndex(Node* node) { return PastValueIndex(node); }
  static int FirstFrameStateIndex(Node* node) { return PastContextIndex(node); }
  static int FirstEffectIndex(Node* node) { return PastFrameStateIndex(node); }
  static int FirstControlIndex(Node* node) { return PastEffectIndex(node); }
  static int PastValueIndex(Node* node);
  static int PastContextIndex(Node* node);
  static int PastFrameStateIndex(Node* node);
  static int PastEffectIndex(Node* node);
  static int PastControlIndex(Node* node);


  // ---------------------------------------------------------------------------
  // Input accessors.

  static Node* GetValueInput(Node* node, int index);
  static Node* GetContextInput(Node* node);
  static Node* GetFrameStateInput(Node* node);
  static Node* GetEffectInput(Node* node, int index = 0);
  static Node* GetControlInput(Node* node, int index = 0);


  // ---------------------------------------------------------------------------
  // Edge kinds.

  static bool IsValueEdge(Edge edge);
  static bool IsContextEdge(Edge edge);
  static bool IsFrameStateEdge(Edge edge);
  static bool IsEffectEdge(Edge edge);
  static bool IsControlEdge(Edge edge);


  // ---------------------------------------------------------------------------
  // Miscellaneous predicates.

  static bool IsCommon(Node* node) {
    return IrOpcode::IsCommonOpcode(node->opcode());
  }
  static bool IsControl(Node* node) {
    return IrOpcode::IsControlOpcode(node->opcode());
  }
  static bool IsConstant(Node* node) {
    return IrOpcode::IsConstantOpcode(node->opcode());
  }
  static bool IsPhi(Node* node) {
    return IrOpcode::IsPhiOpcode(node->opcode());
  }


  // ---------------------------------------------------------------------------
  // Miscellaneous mutators.

  static void ReplaceContextInput(Node* node, Node* context);
  static void ReplaceControlInput(Node* node, Node* control);
  static void ReplaceEffectInput(Node* node, Node* effect, int index = 0);
  static void ReplaceFrameStateInput(Node* node, Node* frame_state);
  static void RemoveNonValueInputs(Node* node);

  // Replace value uses of {node} with {value} and effect uses of {node} with
  // {effect}. If {effect == NULL}, then use the effect input to {node}.
  static void ReplaceWithValue(Node* node, Node* value, Node* effect = nullptr);


  // ---------------------------------------------------------------------------
  // Miscellaneous utilities.

  static Node* FindProjection(Node* node, size_t projection_index);

  // Collect the branch-related projections from a node, such as IfTrue,
  // IfFalse, IfValue and IfDefault.
  //  - Branch: [ IfTrue, IfFalse ]
  //  - Switch: [ IfValue, ..., IfDefault ]
  static void CollectControlProjections(Node* node, Node** proj, size_t count);


  // ---------------------------------------------------------------------------
  // Type Bounds.

  static bool IsTyped(Node* node) {
    Bounds const bounds = node->bounds();
    DCHECK(!bounds.lower == !bounds.upper);
    return bounds.upper;
  }
  static Bounds GetBounds(Node* node) {
    DCHECK(IsTyped(node));
    return node->bounds();
  }
  static void SetBounds(Node* node, Bounds bounds) {
    DCHECK_NOT_NULL(bounds.lower);
    DCHECK_NOT_NULL(bounds.upper);
    node->set_bounds(bounds);
  }
  static void RemoveBounds(Node* node) { node->set_bounds(Bounds()); }
  static bool AllValueInputsAreTyped(Node* node);

 private:
  static inline bool IsInputRange(Edge edge, int first, int count);
};

}  // namespace compiler
}  // namespace internal
}  // namespace v8

#endif  // V8_COMPILER_NODE_PROPERTIES_H_
