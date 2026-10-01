import { filtrarCargos, obtenerRangoCoincidente } from "../busqueda.js";

describe("Autocompletado de cargos", function() {
  const cargos = ["Analista de Datos", "Desarrollador Backend", "Diseñador UX/UI"];

  it("filtra por texto parcial sin distinguir mayúsculas ni tildes", function() {
    expect(filtrarCargos(cargos, "disen")).toEqual(["Diseñador UX/UI"]);
    expect(filtrarCargos(cargos, "ANALISTA")).toEqual(["Analista de Datos"]);
  });

  it("conserva el orden alfabético de las sugerencias recibidas", function() {
    expect(filtrarCargos(cargos, "des")).toEqual(["Desarrollador Backend"]);
  });

  it("devuelve el rango original para resaltar la coincidencia", function() {
    expect(obtenerRangoCoincidente("Diseñador UX/UI", "disen")).toEqual({ inicio: 0, fin: 5 });
  });
});