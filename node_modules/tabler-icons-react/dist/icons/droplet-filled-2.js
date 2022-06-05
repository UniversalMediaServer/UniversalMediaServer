import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function DropletFilled2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-droplet-filled-2",
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
    d: "M6.8 11a6 6 0 1 0 10.396 0l-5.197 -8l-5.2 8z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 14h12"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7.305 17.695l3.695 -3.695"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10.26 19.74l5.74 -5.74l-5.74 5.74z"
  }));
}

export { DropletFilled2 as default };
