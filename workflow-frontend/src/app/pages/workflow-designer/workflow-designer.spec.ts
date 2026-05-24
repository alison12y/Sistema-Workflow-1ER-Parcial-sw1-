import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkflowDesigner } from './workflow-designer';

describe('WorkflowDesigner', () => {
  let component: WorkflowDesigner;
  let fixture: ComponentFixture<WorkflowDesigner>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkflowDesigner],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkflowDesigner);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
