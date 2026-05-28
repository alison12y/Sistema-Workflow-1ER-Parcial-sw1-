export type UmlNodeType =
  | 'INITIAL'
  | 'ACTION'
  | 'DECISION'
  | 'MERGE'
  | 'FORK_JOIN'
  | 'FINAL'
  | 'SWIMLANE';

export interface DiagramNode {
  id: string;
  type: UmlNodeType | string;
  label: string;
  x: number;
  y: number;
  lane?: string;
}

export interface DiagramEdge {
  id: string;
  sourceId: string;
  targetId: string;
  label?: string;
}

export interface ActivityDiagram {
  id?: string;
  policyId?: string;
  name?: string;
  lanes?: string[];
  nodes: DiagramNode[];
  edges: DiagramEdge[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ActivityDiagramSaveRequest {
  policyId: string;
  name: string;
  lanes?: string[];
  nodes: DiagramNode[];
  edges: DiagramEdge[];
}
