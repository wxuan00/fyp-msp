import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'maskCard', standalone: true })
export class MaskCardPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '-';
    const digits = value.replace(/\s/g, '');
    if (digits.length < 4) return value;
    return '**** **** **** ' + digits.slice(-4);
  }
}
