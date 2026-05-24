import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Kpis } from './kpis';

describe('Kpis', () => {
  let component: Kpis;
  let fixture: ComponentFixture<Kpis>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Kpis],
    }).compileComponents();

    fixture = TestBed.createComponent(Kpis);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
