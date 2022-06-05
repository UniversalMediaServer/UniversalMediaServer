import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BrandPython(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-brand-python",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 9h-7a2 2 0 0 0 -2 2v4a2 2 0 0 0 2 2h3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 15h7a2 2 0 0 0 2 -2v-4a2 2 0 0 0 -2 -2h-3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 9v-4a2 2 0 0 1 2 -2h4a2 2 0 0 1 2 2v5a2 2 0 0 1 -2 2h-4a2 2 0 0 0 -2 2v5a2 2 0 0 0 2 2h4a2 2 0 0 0 2 -2v-4"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "11",
    y1: "6",
    x2: "11",
    y2: "6.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "13",
    y1: "18",
    x2: "13",
    y2: "18.01"
  }));
}

export { BrandPython as default };
