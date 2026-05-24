import { Component } from '@angular/core';
import { Sidebar } from '../../layout/sidebar/sidebar';
import { Navbar } from '../../layout/navbar/navbar';

@Component({
  selector: 'app-policies',
  imports: [Sidebar, Navbar],
  templateUrl: './policies.html',
  styleUrl: './policies.css'
})
export class Policies {}