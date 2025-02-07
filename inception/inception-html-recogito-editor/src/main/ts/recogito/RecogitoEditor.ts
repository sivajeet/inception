/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import '@recogito/recogito-js/dist/recogito.min.css'
import { Recogito } from '@recogito/recogito-js/src';
import type { AnnotationEditor, CompactAnnotatedText, CompactSpan, DiamAjax, VID } from "@inception-project/inception-js-api";
import { CompactRelation } from '@inception-project/inception-js-api/src/model/compact/CompactRelation';
import "./RecogitoEditor.css"

interface WebAnnotation {
  id: string;
  type: string;
  motivation?: string;
  target: WebAnnotationTextPositionSelector | Array<WebAnnotationAnnotationTarget>;
  body: Array<WebAnnotationBodyItem>;
}

interface WebAnnotationBodyItem {
  type: string;
  value: string;
  purpose: string;
}

interface WebAnnotationAnnotationTarget {
  id: string;
}

interface WebAnnotationTextPositionSelector {
  selector: {
    start: number;
    end: number;
  }
}

export class RecogitoEditor implements AnnotationEditor {
  private ajax: DiamAjax;
  private recogito: Recogito;

  public constructor(element: Element, ajax: DiamAjax) {
    this.ajax = ajax;

    this.recogito = new Recogito({
      content: element,
      disableEditor: true,
      mode: 'pre'
    });

    this.recogito.on('createAnnotation', annotation => this.createAnnotation(annotation));
    this.recogito.on('selectAnnotation', annotation => this.selectAnnotation(annotation));

    element.addEventListener('contextmenu', e => this.openContextMenu(e));

    // Prevent right-click from triggering a selection event in RecogitoJS
    element.addEventListener('mousedown', e => this.cancelRightClick(e), {capture: true});
    element.addEventListener('mouseup', e => this.cancelRightClick(e), {capture: true});
    element.addEventListener('mouseclick', e => this.cancelRightClick(e), {capture: true});

    this.loadAnnotations();
  }

  private cancelRightClick(e: Event): void {
    if (e instanceof MouseEvent) {
      if (e.button === 2) {
        e.preventDefault();
        e.stopPropagation();
      }
    }
  }

  private openContextMenu(e): void {
    if (!(e instanceof MouseEvent) || !(e.target instanceof Element)) {
      return
    }

    const target = e.target as Element;
    const annotationSpan = target.closest('.r6o-annotation');

    if (!annotationSpan || !annotationSpan.getAttribute('data-id')) {
      return
    }

    // The RecogitoJS annotation IDs start with a hash `#` which we need to remove
    const annotationId = annotationSpan.getAttribute('data-id')?.substring(1) as VID

    this.ajax.openContextMenu(annotationId, e)
    e.preventDefault()
    e.stopPropagation()
  }

  public loadAnnotations(): void {
    this.ajax.loadAnnotations().then((doc: CompactAnnotatedText) => {
      if (!this.recogito) {
        console.error("It seems RecogitoJS has not yet been initialized", this);
        return;
      }

      const webAnnotations: Array<WebAnnotation> = [];

      if (doc.spans) {
        webAnnotations.push(...this.compactSpansToWebAnnotation(doc.spans));
      }

      if (doc.relations) {
        webAnnotations.push(...this.compactRelationsToWebAnnotation(doc.relations));
      }

      console.info(`Loaded ${webAnnotations.length} annotations from server`);
      this.recogito.setAnnotations(webAnnotations);
    });
  }

  private compactSpansToWebAnnotation(spans: Array<CompactSpan>): Array<WebAnnotation> {
    return spans.map(span => {
      return {
        id: '#' + span[0],
        type: 'Annotation',
        body: [{
          type: 'TextualBody',
          purpose: 'tagging',
          value: span[2].l || ""
        }],
        target: {
          selector: { type: "TextPositionSelector", start: span[1][0][0], end: span[1][0][1] }
        }
      }
    })
  }

  private compactRelationsToWebAnnotation(relations: Array<CompactRelation>): Array<WebAnnotation> {
    return relations.map(relation => {
      return {
        id: '#' + relation[0],
        type: 'Annotation',
        body: [{
          type: 'TextualBody',
          purpose: 'tagging',
          value: relation[2].l || ""
        }],
        motivation: 'linking',
        target: [
          { id: '#' + relation[1][0][0] },
          { id: '#' + relation[1][1][0] }
        ]
      }
    })
  }

  public destroy(): void {
    this.recogito.destroy();
  }

  private createAnnotation(annotation): void {
    let target = annotation.target;
    let text: string, begin: number, end: number;

    for (let i = 0; i < target.selector.length; i++) {
      if (target.selector[i].type === "TextQuoteSelector") {
        text = target.selector[i].exact;
      }
      if (target.selector[i].type === "TextPositionSelector") {
        begin = target.selector[i].start;
        end = target.selector[i].end;
      }
    }

    this.ajax.createSpanAnnotation([[begin, end]], text);
  }

  private selectAnnotation(annotation): void {
    // The RecogitoJS annotation IDs start with a hash `#` which we need to remove
    this.ajax.selectAnnotation(annotation.id.substring('1'));
  }
}
