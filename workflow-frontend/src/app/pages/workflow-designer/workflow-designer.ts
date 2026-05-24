import { Component } from '@angular/core';
import { Sidebar } from '../../layout/sidebar/sidebar';
import { Navbar } from '../../layout/navbar/navbar';

@Component({
  selector: 'app-workflow-designer',
  imports: [Sidebar, Navbar],
  templateUrl: './workflow-designer.html',
  styleUrl: './workflow-designer.css'
})
export class WorkflowDesigner {}