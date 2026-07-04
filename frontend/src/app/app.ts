import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly navigationItems = [
    {
      label: 'Dashboard',
      description: 'Visao ao vivo da operacao',
      route: '/dashboard'
    },
    {
      label: 'Atendimentos',
      description: 'Gerenciamento e historico',
      route: '/atendimentos'
    },
    {
      label: 'Atendentes',
      description: 'Capacidade, status e time',
      route: '/atendentes'
    }
  ];
}
