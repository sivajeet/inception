import { VID } from '@inception-project/inception-js-api'
import { dispatchWindowEvent } from '../../../shared/util'
import AbstractAnnotation from './abstract'

/**
 * Annotation Container.
 */
export default class AnnotationContainer {
  set = new Set<AbstractAnnotation>()

  /**
   * Add an annotation to the container.
   */
  add (annotation: AbstractAnnotation) {
    this.set.add(annotation)
    dispatchWindowEvent('annotationUpdated')
  }

  /**
   * Remove the annotation from the container.
   */
  remove (annotation: AbstractAnnotation) {
    this.set.delete(annotation)
    dispatchWindowEvent('annotationUpdated')
  }

  /**
   * Remove all annotations.
   */
  clear () {
    this.set.forEach(a => a.destroy())
    this.set = new Set()
  }

  /**
   * Get all annotations from the container.
   */
  getAllAnnotations () : AbstractAnnotation[] {
    const list = []
    this.set.forEach(a => list.push(a))
    return list
  }

  /**
   * Find an annotation by the id which an annotation has.
   */
  findById (vid: VID) : AbstractAnnotation {
    let annotation
    this.set.forEach(a => {
      if (a.vid === vid) {
        annotation = a
      }
    })
    return annotation
  }
}
